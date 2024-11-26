package eu.isygoit.mapper;

import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AccountMapper extends EntityMapper<Account, UpdateAccountRequestDto> {

}
