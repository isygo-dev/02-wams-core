package eu.isygoit.mapper;


import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Category mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface CategoryMapper extends EntityMapper<Category, CategoryDto> {
}
