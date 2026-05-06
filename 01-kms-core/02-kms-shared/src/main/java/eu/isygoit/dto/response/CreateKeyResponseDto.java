package eu.isygoit.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import eu.isygoit.enums.IEnumKeyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * The type Create key response dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class CreateKeyResponseDto {

    private String keyId;

    private String arn;

    private IEnumKeyStatus.Types status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}

