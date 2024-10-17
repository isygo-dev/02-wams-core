package eu.isygoit.mapper;

import eu.isygoit.dto.data.DomainDto;
import eu.isygoit.model.Domain;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Domain mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface DomainMapper extends EntityMapper<Domain, DomainDto> {
}

