package eu.isygoit.mapper;

import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.model.AppNextCode;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface App next code mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AppNextCodeMapper extends EntityMapper<AppNextCode, NextCodeDto> {

}
