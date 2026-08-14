package eu.isygoit.mapper;

import eu.isygoit.dto.common.LinkedFileRequestDto;
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

    @Override
    @Mapping(target = "categoryNames", source = "categories", qualifiedByName = "mapCategoriesToNames")
    LinkedFileResponseDto entityToDto(LinkedFile entity);

    @Named("mapCategoriesToNames")
    default List<String> mapCategoriesToNames(List<Category> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toList());
    }
}