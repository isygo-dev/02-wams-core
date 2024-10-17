package eu.isygoit.mapper;

import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Min account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface MinAccountMapper extends EntityMapper<Account, MinAccountDto> {


}
