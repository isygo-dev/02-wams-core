package eu.isygoit.mapper;

import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.model.SenderConfig;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Sender config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface SenderConfigMapper extends EntityMapper<SenderConfig, SenderConfigDto> {

}
