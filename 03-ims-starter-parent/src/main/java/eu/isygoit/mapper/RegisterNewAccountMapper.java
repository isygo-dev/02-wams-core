package eu.isygoit.mapper;

import eu.isygoit.dto.request.RegisterNewAccountDto;
import eu.isygoit.model.RegistredUser;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Register new account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface RegisterNewAccountMapper extends EntityMapper<RegistredUser, RegisterNewAccountDto> {
}
