package eu.isygoit.mapper;

import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.model.PasswordConfig;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Password config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface PasswordConfigMapper extends EntityMapper<PasswordConfig, PasswordConfigDto> {

}
