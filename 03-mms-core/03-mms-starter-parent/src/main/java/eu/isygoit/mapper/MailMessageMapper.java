package eu.isygoit.mapper;

import eu.isygoit.dto.data.MailMessageDto;
import eu.isygoit.model.MailMessage;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Mail message mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface MailMessageMapper extends EntityMapper<MailMessage, MailMessageDto> {

}
