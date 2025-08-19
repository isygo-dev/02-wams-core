package eu.isygoit.dto.data;


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
public class DomainAdminDto {

    private String email;
    private String phone;
    private String firstName;
    private String LastName;
}
