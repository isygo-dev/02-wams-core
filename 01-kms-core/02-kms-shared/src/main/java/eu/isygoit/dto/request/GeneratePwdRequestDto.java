package eu.isygoit.dto.request;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Generate pwd request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class GeneratePwdRequestDto {

    @NotEmpty
    private String tenant;
    private String tenantUrl;
    @NotEmpty
    private String email;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String fullName;
}
