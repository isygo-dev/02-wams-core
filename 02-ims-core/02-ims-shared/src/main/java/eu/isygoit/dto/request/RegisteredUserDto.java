package eu.isygoit.dto.request;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Register new account dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RegisteredUserDto extends AuditableDto<Long> {

    private Long id;
    @Builder.Default
    private String tenant = TenantConstants.DEFAULT_TENANT_NAME;
    @Builder.Default
    private String origin = IEnumAccountOrigin.Types.SYS_ADMIN.name();

    @NotEmpty
    private String firstName;
    @NotEmpty
    private String lastName;
    @NotEmpty
    private String email;
    @NotEmpty
    private String phoneNumber;

    private String functionRole;
}
