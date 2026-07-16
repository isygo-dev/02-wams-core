package eu.isygoit.api;

import eu.isygoit.com.rest.api.IMappedCrudApi;
import eu.isygoit.dto.request.RegisteredUserDto;

/**
 * The interface Account controller api.
 */
public interface RegisteredUserServiceApi extends IMappedCrudApi<Long, RegisteredUserDto, RegisteredUserDto> {

}
