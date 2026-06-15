package eu.isygoit.dto;

import eu.isygoit.dto.extendable.AddressModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Address dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AddressDto extends AddressModelDto<Long> {

    private Long id;
}
