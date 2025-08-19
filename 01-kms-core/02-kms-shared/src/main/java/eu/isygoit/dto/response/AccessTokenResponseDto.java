package eu.isygoit.dto.response;


import eu.isygoit.enums.IEnumPasswordStatus;
import eu.isygoit.enums.IEnumWebToken;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Access token response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccessTokenResponseDto {

    @NotNull
    private IEnumPasswordStatus.Types status;
    @NotNull
    private IEnumWebToken.Types tokenType;
    @NotEmpty
    private String accessToken;
    @NotEmpty
    private String refreshToken;
    @NotEmpty
    private String authorityToken;
}
