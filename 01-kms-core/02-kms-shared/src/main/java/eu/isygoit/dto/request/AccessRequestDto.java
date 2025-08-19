package eu.isygoit.dto.request;


import eu.isygoit.enums.IEnumAuth;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * The type Access request dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccessRequestDto {

    @NotEmpty
    private String tenant;
    @NotEmpty
    private String application;
    @NotEmpty
    private String userName;
    @NotEmpty
    private String password;
    @NotNull
    private Boolean isAdmin;
    @NotNull
    private IEnumAuth.Types authType;
    //@NotEmpty
    private List<String> authorities;
}
