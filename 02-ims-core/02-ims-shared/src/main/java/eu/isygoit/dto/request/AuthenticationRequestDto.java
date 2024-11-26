package eu.isygoit.dto.request;


import eu.isygoit.dto.extendable.AbstractDto;
import eu.isygoit.enums.IEnumAuth;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Auth request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AuthenticationRequestDto extends AbstractDto {

    @NotEmpty
    private String domain;
    @NotEmpty
    private String application;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String password;
    @NotNull
    private IEnumAuth.Types authType;
}
