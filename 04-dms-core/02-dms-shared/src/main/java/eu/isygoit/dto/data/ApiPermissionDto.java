package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.ApiPermissionModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * The type Api permission dto.
 */
@Data
@AllArgsConstructor
@SuperBuilder
public class ApiPermissionDto extends ApiPermissionModelDto<Long> {
}
