package eu.isygoit.mapper;

import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.model.StorageConfig;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Storage config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface StorageConfigMapper extends EntityMapper<StorageConfig, StorageConfigDto> {

}
