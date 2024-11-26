package eu.isygoit.mapper;

import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.model.RoleInfo;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;


/**
 * The interface Role info mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface RoleInfoMapper extends EntityMapper<RoleInfo, RoleInfoDto> {

}