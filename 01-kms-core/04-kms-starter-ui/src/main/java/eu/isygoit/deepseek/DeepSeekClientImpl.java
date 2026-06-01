package eu.isygoit.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.isygoit.deepseek.config.DeepSeekConfig;
import eu.isygoit.deepseek.exception.*;
import eu.isygoit.deepseek.model.ChatRequest;
import eu.isygoit.deepseek.model.ChatResponse;
import eu.isygoit.deepseek.streaming.StreamHandler;
import eu.isygoit.deepseek.streaming.StreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SubmissionPublisher;

class DeepSeekClientImpl implements DeepSeekClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekClientImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DeepSeekConfig config;
    private final HttpClient httpClient;

    DeepSeekClientImpl(DeepSeekConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .version(config.httpVersion())
                .connectTimeout(config.timeout())
                .build();
    }

    private static DeepSeekException mapStatusCodeToException(int statusCode, String responseBody) {
        String message = String.format("HTTP %d: %s", statusCode, responseBody);
        return switch (statusCode) {
            case 401 -> new AuthenticationException(message);
            case 429 -> new RateLimitException(message);
            case 400 -> new InvalidRequestException(message);
            default -> new ApiServerException(message, statusCode);
        };
    }

    private static boolean isRetryable(DeepSeekException e) {
        return e instanceof RateLimitException || e instanceof ApiServerException;
    }

    @Override
    public ChatResponse chat(ChatRequest request) throws DeepSeekException {
        // Force stream = false
        ChatRequest nonStreamRequest = new ChatRequest(
                request.getModel(),
                request.getMessages(),
                request.getTemperature(),
                request.getMaxTokens(),
                false
        );
        return executeWithRetry(nonStreamRequest);
    }

    @Override
    public void chatStream(ChatRequest request, StreamListener listener) throws DeepSeekException {
        // Force stream = true
        ChatRequest streamRequest = new ChatRequest(
                request.getModel(),
                request.getMessages(),
                request.getTemperature(),
                request.getMaxTokens(),
                true
        );
        executeStream(streamRequest, listener);
    }

    private ChatResponse executeWithRetry(ChatRequest request) throws DeepSeekException {
        Exception lastException = null;
        for (int attempt = 1; attempt <= config.retryCount(); attempt++) {
            try {
                return executeOnce(request);
            } catch (DeepSeekException e) {
                lastException = e;
                if (isRetryable(e) && attempt < config.retryCount()) {
                    log.warn("Retryable error, attempt {}/{}: {}", attempt, config.retryCount(), e.getMessage());
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new DeepSeekIoException("Interrupted during retry", ie);
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new DeepSeekIoException("Max retries exhausted", lastException);
    }

    private ChatResponse executeOnce(ChatRequest request) throws DeepSeekException {
        try {
            String jsonBody = MAPPER.writeValueAsString(request);
            log.debug("Request JSON: {}", jsonBody);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(config.timeout())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (config.apiKey() != null && !config.apiKey().isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + config.apiKey());
            }
            if (config.openRouterReferer() != null && !config.openRouterReferer().isBlank()) {
                reqBuilder.header("HTTP-Referer", config.openRouterReferer());
            }
            if (config.openRouterTitle() != null && !config.openRouterTitle().isBlank()) {
                reqBuilder.header("X-Title", config.openRouterTitle());
            }

            HttpRequest httpRequest = reqBuilder.build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String rawJson = response.body();
            log.debug("Response status: {}, body: {}", response.statusCode(), rawJson);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                ChatResponse chatResponse = MAPPER.readValue(rawJson, ChatResponse.class);
                if (chatResponse.firstChoiceMessage().content() == null) {
                    log.warn("Content is null. Full response: {}", rawJson);
                }
                return chatResponse;
            } else {
                throw mapStatusCodeToException(response.statusCode(), rawJson);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DeepSeekIoException("Communication error: " + e.getMessage(), e);
        }
    }

    private void executeStream(ChatRequest request, StreamListener listener) throws DeepSeekException {
        try {
            String jsonBody = MAPPER.writeValueAsString(request);
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .timeout(config.timeout())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (config.apiKey() != null && !config.apiKey().isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + config.apiKey());
            }
            if (config.openRouterReferer() != null && !config.openRouterReferer().isBlank()) {
                reqBuilder.header("HTTP-Referer", config.openRouterReferer());
            }
            if (config.openRouterTitle() != null && !config.openRouterTitle().isBlank()) {
                reqBuilder.header("X-Title", config.openRouterTitle());
            }

            HttpRequest httpRequest = reqBuilder.build();
            CompletableFuture<Void> future = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        if (response.statusCode() != 200) {
                            try {
                                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                                listener.onError(mapStatusCodeToException(response.statusCode(), errorBody));
                            } catch (IOException e) {
                                listener.onError(e);
                            }
                            return;
                        }
                        try (var body = response.body();
                             var reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
                            String line;
                            var handler = new StreamHandler(listener);
                            var publisher = new SubmissionPublisher<String>();
                            publisher.subscribe(handler);
                            while ((line = reader.readLine()) != null) {
                                publisher.submit(line);
                            }
                            publisher.close();
                        } catch (IOException e) {
                            listener.onError(e);
                        }
                    });
            future.join();
        } catch (Exception e) {
            throw new DeepSeekIoException("Failed to initiate stream: " + e.getMessage(), e);
        }
    }
}