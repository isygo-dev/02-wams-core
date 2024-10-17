package eu.isygoit.dto.data;

import eu.isygoit.dto.extendable.AbstractAuditableDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * The type Sender config dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class SenderConfigDto extends AbstractAuditableDto<Long> {

    private String domain;
    private String host;
    private String port;
    private String username;
    private String password;
    private String transportProtocol;
    private String smtpAuth;
    private Boolean smtpStarttlsEnable;
    private Boolean smtpStarttlsRequired;
    private Boolean debug;
}
