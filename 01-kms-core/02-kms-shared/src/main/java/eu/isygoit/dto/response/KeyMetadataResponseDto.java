package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.isygoit.enums.IEnumKeyPurpose;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * The type Key metadata response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class KeyMetadataResponseDto {

    private String keyId;

    private IEnumKeyStatus.Types status;

    private IEnumKeySpec.Types keySpec;

    private IEnumKeyPurpose.Types keyPurpose;

    private String currentVersion;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String alias;

    private String description;
}

