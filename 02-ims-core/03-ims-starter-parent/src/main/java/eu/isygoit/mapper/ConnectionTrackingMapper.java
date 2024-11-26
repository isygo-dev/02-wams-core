package eu.isygoit.mapper;

import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.model.ConnectionTracking;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;


/**
 * The interface Connection tracking mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface ConnectionTrackingMapper extends EntityMapper<ConnectionTracking, ConnectionTrackingDto> {

}
