package eu.isygoit.mapper;

import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.model.AccountDetails;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Account details mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AccountDetailsMapper extends EntityMapper<AccountDetails, AccountDetailsDto> {
}
