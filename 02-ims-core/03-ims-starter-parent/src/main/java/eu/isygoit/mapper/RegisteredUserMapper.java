package eu.isygoit.mapper;

import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.model.RegisteredUser;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Register new account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface RegisteredUserMapper extends EntityMapper<RegisteredUser, RegisteredUserDto> {
}
