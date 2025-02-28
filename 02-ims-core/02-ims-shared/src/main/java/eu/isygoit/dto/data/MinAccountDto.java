package eu.isygoit.dto.data;


import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.extendable.AccountModelDto;
import eu.isygoit.enums.IEnumAccountSystemStatus;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumWSStatus;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

/**
 * The type Min account dto.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MinAccountDto extends AccountModelDto<Long> {

    @NotEmpty
    private String domain;
    private String imagePath;
    private String functionRole;
    private Boolean isAdmin;
    private Date lastConnectionDate;
    @Builder.Default
    private IEnumEnabledBinaryStatus.Types adminStatus = IEnumEnabledBinaryStatus.Types.ENABLED;
    @Builder.Default
    private IEnumAccountSystemStatus.Types systemStatus = IEnumAccountSystemStatus.Types.IDLE;

    private IEnumAuth.Types authType;
    @Builder.Default
    private String accountType = AccountTypeConstants.DOMAIN_USER;

    private AccountDetailsDto accountDetails;

    //Chat status info
    @Builder.Default
    private IEnumWSStatus.Types chatStatus = IEnumWSStatus.Types.DISCONNECTED;

    /**
     * Gets full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return this.getAccountDetails() != null ? this.accountDetails.getFullName() : null;
    }
}
