package eu.isygoit.mapper;

import eu.isygoit.dto.data.ThemeDto;
import eu.isygoit.model.Theme;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Theme mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface ThemeMapper extends EntityMapper<Theme, ThemeDto> {
}
