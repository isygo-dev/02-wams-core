package eu.isygoit.deepseek;


import eu.isygoit.deepseek.config.DeepSeekConfig;
import eu.isygoit.deepseek.exception.DeepSeekException;
import eu.isygoit.deepseek.model.ChatRequest;
import eu.isygoit.deepseek.model.ChatResponse;
import eu.isygoit.deepseek.streaming.StreamListener;

public interface DeepSeekClient {
    static DeepSeekClient create(DeepSeekConfig config) {
        return new DeepSeekClientImpl(config);
    }

    ChatResponse chat(ChatRequest request) throws DeepSeekException;

    void chatStream(ChatRequest request, StreamListener listener) throws DeepSeekException;
}