package eu.isygoit.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;


/**
 * The type Token config dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TokenConfigDto extends AuditableDto<Long> {

    private Long id;
    private String tenant;
    private String code;
    private IEnumToken.Types tokenType;
    private String issuer;
    private List<String> audience;
    private String signatureAlgorithm;

    @JsonProperty(access = WRITE_ONLY)
    private String secretKey;
    @JsonProperty(access = WRITE_ONLY)
    private String publicKey;

    private Integer lifeTimeInMs;

    private String kmsKeyId;
}
