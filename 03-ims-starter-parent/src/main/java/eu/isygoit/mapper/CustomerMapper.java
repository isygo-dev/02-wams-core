package eu.isygoit.mapper;

import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Customer mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface CustomerMapper extends EntityMapper<Customer, CustomerDto> {
}

