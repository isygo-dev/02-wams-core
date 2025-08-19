package eu.isygoit.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Check pwd request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CheckPwdRequestDto {

    @NotEmpty
    private String tenant;
    @NotEmpty
    private String password;
}
