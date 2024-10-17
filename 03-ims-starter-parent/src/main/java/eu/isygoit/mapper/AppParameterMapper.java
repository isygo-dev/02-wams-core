package eu.isygoit.mapper;

import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.model.AppParameter;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface App parameter mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AppParameterMapper extends EntityMapper<AppParameter, AppParameterDto> {
}
