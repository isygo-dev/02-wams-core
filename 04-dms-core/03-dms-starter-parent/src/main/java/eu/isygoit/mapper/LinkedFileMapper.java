package eu.isygoit.mapper;

import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.model.Category;
import eu.isygoit.model.LinkedFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The interface Linked file mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface LinkedFileMapper extends EntityMapper<LinkedFile, LinkedFileResponseDto> {

}