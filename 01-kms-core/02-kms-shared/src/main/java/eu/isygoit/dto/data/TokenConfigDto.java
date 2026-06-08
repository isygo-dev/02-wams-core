package eu.isygoit.dto.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.isygoit.annotation.ExcludeOnResponse;
import eu.isygoit.dto.extendable.AuditableIdAssignableDto;
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
public class TokenConfigDto extends AuditableIdAssignableDto<Long> {

    private Long id;
    private String tenant;
    private String code;
    private IEnumToken.Types tokenType;
    private String issuer;
    private List<String> audience;
    private String signatureAlgorithm;
    private Integer lifeTimeInMs;
    private String kmsKeyId;

    @ExcludeOnResponse
    private String secretKey;
    @ExcludeOnResponse
    private String publicKey;
}
