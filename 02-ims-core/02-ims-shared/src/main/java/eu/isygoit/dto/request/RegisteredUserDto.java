package eu.isygoit.dto.request;

import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.enums.IEnumRegistrationStatus;
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
public class RegisteredUserDto extends AuditableIdAssignableDto<Long> {

    private Long id;
    @Builder.Default
    private String tenant = TenantConstants.DEFAULT_TENANT_NAME;
    @Builder.Default
    private IEnumAccountOrigin.Types origin = IEnumAccountOrigin.Types.SIGNUP;

    @NotEmpty
    private String firstName;
    @NotEmpty
    private String lastName;
    @NotEmpty
    private String email;
    @NotEmpty
    private String phoneNumber;

    @NotEmpty
    private String organisation;

    private String functionRole;

    @Builder.Default
    private IEnumRegistrationStatus.Types status = IEnumRegistrationStatus.Types.NEW;
}
