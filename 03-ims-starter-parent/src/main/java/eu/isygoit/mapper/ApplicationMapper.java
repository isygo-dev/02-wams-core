package eu.isygoit.mapper;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.model.Application;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Application mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface ApplicationMapper extends EntityMapper<Application, ApplicationDto> {
}
