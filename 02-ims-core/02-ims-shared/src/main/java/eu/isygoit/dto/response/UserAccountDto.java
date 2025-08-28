package eu.isygoit.dto.response;

import eu.isygoit.dto.extendable.AuditableDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type User Auth dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class UserAccountDto extends AuditableDto<Long> {

    private Long id;
    @NotEmpty
    private String tenant;
    @NotNull
    private Long tenantId;
    @NotEmpty
    private String code;
    @NotEmpty
    private String fullName;
    @NotEmpty
    private String functionRole;
}
