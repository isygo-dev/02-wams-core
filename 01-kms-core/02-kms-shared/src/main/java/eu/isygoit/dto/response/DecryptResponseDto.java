package eu.isygoit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Decrypt response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class DecryptResponseDto {

    private String plaintext;

    private Long keyId;

    private String keyVersion;
}

