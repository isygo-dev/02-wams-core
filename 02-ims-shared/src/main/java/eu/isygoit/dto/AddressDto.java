package eu.isygoit.dto;

import eu.isygoit.dto.extendable.AddressModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * The type Address dto.
 */
@Data
@AllArgsConstructor
@SuperBuilder
public class AddressDto extends AddressModelDto<Long> {

}
