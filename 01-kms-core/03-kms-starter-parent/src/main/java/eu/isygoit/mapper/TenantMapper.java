package eu.isygoit.mapper;

import eu.isygoit.dto.data.KmsTenantDto;
import eu.isygoit.model.Tenant;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Domain mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface TenantMapper extends EntityMapper<Tenant, KmsTenantDto> {

}
