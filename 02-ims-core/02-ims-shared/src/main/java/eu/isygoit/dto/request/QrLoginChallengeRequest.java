package eu.isygoit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class QrLoginChallengeRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{43}")
    private String challengeId;
}
