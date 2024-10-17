package eu.isygoit.mapper;

import eu.isygoit.dto.data.ApiPermissionDto;
import eu.isygoit.model.ApiPermission;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Api permission mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface ApiPermissionMapper extends EntityMapper<ApiPermission, ApiPermissionDto> {
}
