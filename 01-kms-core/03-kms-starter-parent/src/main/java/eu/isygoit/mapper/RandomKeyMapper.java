package eu.isygoit.mapper;

import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.dto.request.UpdateAccountRequestDto;
import eu.isygoit.model.Account;
import eu.isygoit.model.RandomKey;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;

/**
 * The interface Account mapper.
 */
@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, componentModel = "spring")
public interface RandomKeyMapper extends EntityMapper<RandomKey, RandomKeyDto> {

}
