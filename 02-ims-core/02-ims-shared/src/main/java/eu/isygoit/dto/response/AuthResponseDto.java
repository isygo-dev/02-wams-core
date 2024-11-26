package eu.isygoit.dto.response;


import eu.isygoit.dto.common.SystemInfoDto;
import eu.isygoit.dto.data.ThemeDto;
import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumWebToken;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Auth response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AuthResponseDto extends AbstractAuditableDto<Long> {

    @NotNull
    private IEnumWebToken.Types tokenType;
    @NotEmpty
    private String accessToken;
    @NotEmpty
    private String refreshToken;
    @NotEmpty
    private String authorityToken;

    private UserDataResponseDto userDataResponseDto;
    private SystemInfoDto systemInfo;
    private ThemeDto theme;
}
