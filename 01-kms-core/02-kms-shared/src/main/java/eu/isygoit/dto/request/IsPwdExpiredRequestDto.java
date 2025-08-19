package eu.isygoit.dto.request;


import eu.isygoit.enums.IEnumAuth;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Is pwd expired request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class IsPwdExpiredRequestDto {

    @NotEmpty
    private String tenant;
    @NotEmpty
    private String email;
    @NotEmpty
    private String userName;
    @NotNull
    private IEnumAuth.Types authType;
}
