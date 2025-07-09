package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.TenantModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * The type Kms tenant dto.
 */
@Data
@AllArgsConstructor
@SuperBuilder
public class KmsDomainDto extends TenantModelDto<Long> {

}
