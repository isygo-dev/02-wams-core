package eu.isygoit.deepseek.streaming;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.Flow;

/**
 * Handles Server-Sent Events (SSE) from the DeepSeek streaming API.
 */
public class StreamHandler implements Flow.Subscriber<String> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final StreamListener listener;
    private Flow.Subscription subscription;

    public StreamHandler(StreamListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(String line) {
        try {
            if (line.trim().isEmpty()) {
                subscription.request(1);
                return;
            }
            // SSE lines start with "data: "
            if (line.startsWith("data: ")) {
                String json = line.substring(6);
                if ("[DONE]".equals(json)) {
                    listener.onComplete();
                    subscription.cancel();
                    return;
                }
                JsonNode root = mapper.readTree(json);
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode delta = choices.get(0).path("delta");
                    String content = delta.path("content").asText();
                    if (!content.isEmpty()) {
                        listener.onToken(content);
                    }
                }
            }
            subscription.request(1);
        } catch (Exception e) {
            listener.onError(e);
            subscription.cancel();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        listener.onError(throwable);
    }

    @Override
    public void onComplete() {
        listener.onComplete();
    }
}