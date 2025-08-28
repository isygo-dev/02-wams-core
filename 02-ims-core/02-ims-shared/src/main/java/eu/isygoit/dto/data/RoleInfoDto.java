package eu.isygoit.dto.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import eu.isygoit.dto.extendable.AuditableDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Role info dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class RoleInfoDto extends AuditableDto<Long> {

    private Long id;
    @NotEmpty
    private String tenant;
    private String templateCode;
    private String code;
    @NotEmpty
    private String name;
    @NotNull
    @Builder.Default
    private Integer level = 0;
    private String description;
    @Builder.Default
    private Integer numberOfUsers = 0;
    private List<ApplicationDto> allowedTools;
    @JsonIgnore
    private List<ApiPermissionDto> permissions;
    private List<RolePermissionDto> rolePermission;
}
