package eu.isygoit.mapper;

import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.model.Annex;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Annex mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface AnnexMapper extends EntityMapper<Annex, AnnexDto> {

}
