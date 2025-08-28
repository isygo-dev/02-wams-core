package eu.isygoit.dto;

import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumContact;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Contact dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ContactDto extends AuditableDto<Long> {

    private Long id;
    private IEnumContact.Types type;
    private String value;
}
