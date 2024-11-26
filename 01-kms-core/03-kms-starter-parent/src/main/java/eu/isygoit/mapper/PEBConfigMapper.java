package eu.isygoit.mapper;

import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.model.PEBConfig;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Peb config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface PEBConfigMapper extends EntityMapper<PEBConfig, PEBConfigDto> {

}
