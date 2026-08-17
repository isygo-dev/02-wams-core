package eu.isygoit.mapper;

import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.s3.config.S3Config;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Storage config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface S3ConfigMapper extends EntityMapper<StorageConfig, S3Config> {

}
