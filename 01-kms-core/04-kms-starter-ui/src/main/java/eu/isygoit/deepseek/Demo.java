package eu.isygoit.deepseek;

import eu.isygoit.deepseek.config.DeepSeekConfig;
import eu.isygoit.deepseek.exception.DeepSeekException;
import eu.isygoit.deepseek.model.ChatRequest;
import eu.isygoit.deepseek.model.Message;
import eu.isygoit.deepseek.streaming.StreamListener;

import java.net.http.HttpClient;
import java.time.Duration;

public class Demo {
    public static void main(String[] args) {
        //Get your key from : https://openrouter.ai/workspaces/default/keys
        String openRouterKey = "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"; //System.getenv("OPENROUTER_API_KEY");
        //String openRouterKey = System.getenv("OPENROUTER_API_KEY");
        if (openRouterKey == null || openRouterKey.isBlank()) {
            System.err.println("Please set OPENROUTER_API_KEY environment variable.\n" +
                    "** Get your key from : https://openrouter.ai/keys");

            // Automatically open the browser to the keys page
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://openrouter.ai/keys"));
            } catch (Exception e) {
                System.err.println("Could not open browser automatically: " + e.getMessage());
            }

            System.exit(1);
        }

        var config = DeepSeekConfig.builder()
                .apiKey(openRouterKey)
                .baseUrl("https://openrouter.ai/api/v1")
                .openRouterReferer("https://your-site.com")
                .openRouterTitle("My Java App")
                .timeout(Duration.ofSeconds(30))
                .retryCount(3)
                .httpVersion(HttpClient.Version.HTTP_2)
                .build();

        DeepSeekClient client = DeepSeekClient.create(config);

        ChatRequest request = ChatRequest.builder()
                .model("openrouter/free")
                .message(Message.system("You are a helpful Java expert."))
                .message(Message.user("Explain Java record in one sentence."))
                .temperature(0.7)
                .maxTokens(150)
                .build();


        try {
            var response = client.chat(request);
            System.out.println("Response: " + response.firstChoiceMessage().content());

            System.out.println("\n--- Streaming ---");
            client.chatStream(request, new StreamListener() {
                @Override
                public void onToken(String token) {
                    System.out.print(token);
                }
                @Override
                public void onComplete() {
                    System.out.println("\n[Done]");
                }
                @Override
                public void onError(Throwable t) {
                    System.err.println("Stream error: " + t.getMessage());
                }
            });
        } catch (DeepSeekException e) {
            System.err.println("API error: " + e.getMessage());
        }
    }
}