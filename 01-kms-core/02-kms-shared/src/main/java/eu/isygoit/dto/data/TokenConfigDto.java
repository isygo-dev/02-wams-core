package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import eu.isygoit.enums.IEnumToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


/**
 * The type Token config dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class TokenConfigDto extends AbstractAuditableDto<Long> {

    private Long id;
    private String tenant;
    private String code;
    private IEnumToken.Types tokenType;
    private String issuer;
    private String audience;
    private String signatureAlgorithm;
    private String secretKey;
}
