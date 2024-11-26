package eu.isygoit.mapper;

import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.model.MsgTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Template mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface MsgTemplateMapper extends EntityMapper<MsgTemplate, MsgTemplateDto> {
}
