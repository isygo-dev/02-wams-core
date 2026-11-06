package eu.isygoit.deepseek.model;

import eu.isygoit.exception.ServiceOperationException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatResponse(
        String id,
        String object,
        long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public Message firstChoiceMessage() {
        if (choices == null || choices.isEmpty()) {
            throw new ServiceOperationException("No choices in response");
        }
        return choices.get(0).message();
    }
}