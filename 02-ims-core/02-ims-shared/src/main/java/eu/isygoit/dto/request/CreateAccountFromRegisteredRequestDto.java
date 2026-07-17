package eu.isygoit.dto.request;

import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an account from a registered user.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAccountFromRegisteredRequestDto {

    @NotEmpty
    private String email;

    /**
     * Tenant information for creating a new tenant.
     * If provided, a new tenant will be created with these details.
     */
    @Valid
    private TenantInfo tenantInfo;

    /**
     * Account information for creating the account.
     */
    @NotNull
    private AccountInfo accountInfo;

    /**
     * Tenant DTO for creating/associating with a tenant.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TenantInfo {

        private String industry;
        private String url;
        private String description;
        @Builder.Default
        private IEnumEnabledBinaryStatus.Types adminStatus = IEnumEnabledBinaryStatus.Types.ENABLED;
        private String address;
    }

    /**
     * Account DTO for creating the account.
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AccountInfo {

        /**
         * Account type (e.g., SUPER_ADMIN, TENANT_ADMIN, TENANT_USER)
         */
        @NotEmpty
        @Builder.Default
        private String accountType = "TENANT_USER";

        /**
         * Account language preference.
         */
        @NotNull
        @Builder.Default
        private IEnumLanguage.Types language = IEnumLanguage.Types.EN;

        /**
         * Function role of the user.
         */
        @NotEmpty
        private String functionalRole;

        /**
         * Whether the account should have admin privileges.
         */
        @Builder.Default
        private boolean isAdmin = false;

        /**
         * Admin status for the account.
         */
        @NotNull
        @Builder.Default
        private IEnumEnabledBinaryStatus.Types adminStatus = IEnumEnabledBinaryStatus.Types.ENABLED;

        /**
         * Account details (first name, last name, etc.)
         * If not provided, will be copied from the registered user.
         */
        private AccountDetailsDto accountDetails;
    }
}