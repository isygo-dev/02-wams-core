package eu.isygoit.deepseek.config;

import lombok.Builder;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for DeepSeek / OpenRouter / Ollama clients.
 *
 * @param apiKey            Your API key (not required for Ollama)
 * @param baseUrl           API base URL (default: https://api.deepseek.com/v1)
 * @param timeout           Request timeout (default: 30s)
 * @param retryCount        Number of retries on transient errors (default: 3)
 * @param httpVersion       HTTP version (default: HTTP_2)
 * @param openRouterReferer Optional HTTP-Referer header for OpenRouter
 * @param openRouterTitle   Optional X-Title header for OpenRouter
 */
@Builder
public record DeepSeekConfig(
        //Get your key from : https://openrouter.ai/workspaces/default/keys
        String apiKey,
        String baseUrl,
        Duration timeout,
        int retryCount,
        HttpClient.Version httpVersion,
        String openRouterReferer,
        String openRouterTitle
) {
    public static DeepSeekConfig defaultConfig() {
        return DeepSeekConfig.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .timeout(Duration.ofSeconds(30))
                .retryCount(3)
                .httpVersion(HttpClient.Version.HTTP_2)
                .build();
    }
}