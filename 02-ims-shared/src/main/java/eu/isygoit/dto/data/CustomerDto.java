package eu.isygoit.dto.data;


import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.extendable.CustomerModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Customer dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CustomerDto extends CustomerModelDto<Long> {

    private AddressDto address;
    private String accountCode;
}
