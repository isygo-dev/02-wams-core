package eu.isygoit.mapper;


import eu.isygoit.dto.common.LinkedFileRequestDto;
import eu.isygoit.model.LinkedFile;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Linked file mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface LinkedFileMapper extends EntityMapper<LinkedFile, LinkedFileRequestDto> {
}
