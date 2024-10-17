package eu.isygoit.mapper;


import eu.isygoit.dto.data.PropertyDto;
import eu.isygoit.model.Property;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Property mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface PropertyMapper extends EntityMapper<Property, PropertyDto> {
}
