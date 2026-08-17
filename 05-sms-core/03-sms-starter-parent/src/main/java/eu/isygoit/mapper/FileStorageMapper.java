package eu.isygoit.mapper;

import eu.isygoit.dto.data.FileStorageDto;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.s3.config.S3Config;
import eu.isygoit.s3.object.FileStorage;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Storage config mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface FileStorageMapper extends EntityMapper<FileStorage, FileStorageDto> {

}
