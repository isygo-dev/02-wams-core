package eu.isygoit.mapper;

import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.model.TokenConfig;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Token config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface TokenConfigMapper extends EntityMapper<TokenConfig, TokenConfigDto> {

}
