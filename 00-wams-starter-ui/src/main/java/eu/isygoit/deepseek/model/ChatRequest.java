package eu.isygoit.deepseek.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    @Builder.Default
    private String model = "deepseek-chat";

    @Singular("message")
    private List<Message> messages;

    @Builder.Default
    private Double temperature = 0.7;

    @JsonProperty("max_tokens")
    @Builder.Default
    private Integer maxTokens = 1024;

    private Boolean stream;
}