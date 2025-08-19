package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.ApiPermissionModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@SuperBuilder
public class ApiPermissionDto extends ApiPermissionModelDto<Long> {

    private Long id;
}
