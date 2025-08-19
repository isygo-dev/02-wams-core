package eu.isygoit.dto.response;


import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Access key response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AccessKeyResponseDto {

    @NotNull
    private String key;
    @NotNull
    private int length;
    @NotNull
    private int lifeTime;
}
