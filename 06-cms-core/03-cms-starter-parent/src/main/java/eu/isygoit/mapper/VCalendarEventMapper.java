package eu.isygoit.mapper;

import eu.isygoit.dto.data.VCalendarEventDto;
import eu.isygoit.model.VCalendarEvent;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface V calendar event mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface VCalendarEventMapper extends EntityMapper<VCalendarEvent, VCalendarEventDto> {

}
