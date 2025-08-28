package eu.isygoit.dto.data;

import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.ContactDto;
import eu.isygoit.dto.extendable.AuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Account details dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccountDetailsDto extends AuditableDto<Long> {

    private Long id;
    private String firstName;
    private String lastName;
    private String country;
    private List<ContactDto> contacts;
    private AddressDto address;

    /**
     * Gets full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return new StringBuilder()
                .append(this.getFirstName())
                .append(" ")
                .append(this.getLastName())
                .toString();
    }
}
