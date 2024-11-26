package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.DomainModelDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * The type Kms domain dto.
 */
@Data
@AllArgsConstructor
@SuperBuilder
public class KmsDomainDto extends DomainModelDto<Long> {

}
