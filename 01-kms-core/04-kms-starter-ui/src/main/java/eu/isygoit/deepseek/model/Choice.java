package eu.isygoit.deepseek.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record Choice(
        int index,
        Message message,
        @JsonProperty("finish_reason") String finishReason
) {}