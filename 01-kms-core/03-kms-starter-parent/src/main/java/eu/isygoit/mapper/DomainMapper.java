package eu.isygoit.mapper;

import eu.isygoit.dto.data.KmsDomainDto;
import eu.isygoit.model.KmsDomain;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Domain mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface DomainMapper extends EntityMapper<KmsDomain, KmsDomainDto> {

}
