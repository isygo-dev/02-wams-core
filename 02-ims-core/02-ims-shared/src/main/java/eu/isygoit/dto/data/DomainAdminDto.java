package eu.isygoit.dto.data;


import eu.isygoit.dto.extendable.IdAssignableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Domain dto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DomainAdminDto extends IdAssignableDto<Long> {

    private String email;
    private String phone;
    private String firstName;
    private String LastName;
}
