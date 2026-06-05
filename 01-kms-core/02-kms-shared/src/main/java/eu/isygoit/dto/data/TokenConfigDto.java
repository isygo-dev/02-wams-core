package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AuditableDto;
import eu.isygoit.enums.IEnumToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;


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
    private String secretKey;
    private String publicKey;
    private Integer lifeTimeInMs;

    private String kmsKeyId;
}
