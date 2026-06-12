package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.TenantModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Kms tenant dto.
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class KmsTenantDto extends TenantModelDto<Long> {

    private Long id;
}
