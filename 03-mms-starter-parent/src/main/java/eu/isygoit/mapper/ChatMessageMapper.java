package eu.isygoit.mapper;

import eu.isygoit.dto.data.ChatMessageDto;
import eu.isygoit.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Chat message mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface ChatMessageMapper extends EntityMapper<ChatMessage, ChatMessageDto> {
}
