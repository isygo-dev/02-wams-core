package eu.isygoit.mapper;

import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.model.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AccountMapper extends EntityMapper<Account, AccountDto> {

    /**
     * Profile view of an account. The connection history is deliberately not mapped (mapping it would load the
     * account's whole, ever-growing list): it is served page by page by the profile activity endpoints.
     */
    @Named("profile")
    @Mapping(target = "connectionTracking", ignore = true)
    AccountDto entityToProfileDto(Account account);
}
