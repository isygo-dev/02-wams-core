package eu.isygoit.model;

import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.enums.IEnumWSStatus;
import eu.isygoit.model.extendable.AccountModel;
import eu.isygoit.model.schema.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * The type Account.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
/*
 to_char(
    TO_DATE('01/01/1970 00:00:00','DD/MM/YYYY HH24:MI:SS') + (r.revtstmp /1000/60/60/24),
    'DD/MM/YYYY HH24:MI:SS'
  )
*/
@Entity
@Table(name = SchemaTableConstantName.T_ACCOUNT
        , uniqueConstraints = {
        @UniqueConstraint(name = SchemaUcConstantName.UC_ACCOUNT_CODE, columnNames = {SchemaColumnConstantName.C_CODE})
})
@SQLDelete(sql = "update " + SchemaTableConstantName.T_ACCOUNT + " set " + SchemaColumnConstantName.C_CHECK_CANCEL + "= true , " + SchemaColumnConstantName.C_CANCEL_DATE + " = current_timestamp WHERE id = ?")
@Where(clause = SchemaColumnConstantName.C_CHECK_CANCEL + "=false")
public class Account extends AccountModel<Long> implements IImageEntity {

    @Id
    @SequenceGenerator(name = "account_sequence_generator", sequenceName = "account_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "account_sequence_generator")
    @Column(name = SchemaColumnConstantName.C_ID, updatable = false, nullable = false)
    private Long id;

    @Column(name = SchemaColumnConstantName.C_PHONE_NUMBER, length = SchemaConstantSize.PHONE_NUMBER, nullable = false)
    private String phoneNumber;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'EN'")
    @Column(name = SchemaColumnConstantName.C_LANGUAGE_CODE, length = IEnumLanguage.STR_ENUM_SIZE, nullable = false)
    private IEnumLanguage.Types language = IEnumLanguage.Types.EN;
    @ManyToMany(fetch = FetchType.LAZY /* NO CASCADE */)
    @JoinTable(name = SchemaTableConstantName.T_ASSO_ROLE_INFO_ACCOUNT,
            joinColumns = @JoinColumn(name = SchemaColumnConstantName.C_ROLE_INFO_CODE, referencedColumnName = SchemaColumnConstantName.C_CODE),
            inverseJoinColumns = @JoinColumn(name = SchemaColumnConstantName.C_ACCOUNT_CODE, referencedColumnName = SchemaColumnConstantName.C_CODE))
    private List<RoleInfo> roleInfo;
    @ColumnDefault("'" + AccountTypeConstants.DOMAIN_USER + "'")
    @Column(name = SchemaColumnConstantName.C_FUNCTION_ROLE, length = SchemaConstantSize.S_NAME, nullable = false)
    private String functionRole = AccountTypeConstants.DOMAIN_USER;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL /* CASCADE only for OneToOne*/)
    @JoinColumn(name = SchemaColumnConstantName.C_ACCOUNT_DETAILS_ID, referencedColumnName = SchemaColumnConstantName.C_ID
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_ACCOUNT_REF_DETAILS))
    private AccountDetails accountDetails;
    @Column(name = SchemaColumnConstantName.C_PHOTO)
    private String imagePath;
    @Builder.Default
    @ColumnDefault("'false'")
    @Column(name = SchemaColumnConstantName.C_IS_ADMIN)
    private Boolean isAdmin = Boolean.FALSE;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'OTP'")
    @Column(name = SchemaColumnConstantName.C_AUTH_TYPE, length = IEnumAuth.STR_ENUM_SIZE, nullable = false)
    private IEnumAuth.Types authType = IEnumAuth.Types.OTP;
    @OrderBy(SchemaColumnConstantName.C_LOGIN_DATE + " DESC")
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL /* Cascade only for OneToMany*/)
    @JoinColumn(name = SchemaColumnConstantName.C_ACCOUNT_CODE, referencedColumnName = SchemaColumnConstantName.C_CODE
            , foreignKey = @ForeignKey(name = SchemaFkConstantName.FK_CONNECTION_TRACKING_REF_ACCOUNT))
    private List<ConnectionTracking> connectionTracking;
    @Builder.Default
    @ColumnDefault("'" + AccountTypeConstants.DOMAIN_USER + "'")
    @Column(name = SchemaColumnConstantName.C_TYPE, length = SchemaConstantSize.ACCOUNT_TYPE)
    private String accountType = AccountTypeConstants.DOMAIN_USER;
    @Builder.Default
    @ColumnDefault("'SYS_ADMIN'")
    @Column(name = SchemaColumnConstantName.C_ORIGIN, nullable = false)
    private String origin = IEnumAccountOrigin.Types.SYS_ADMIN.name();

    @Transient
    @Builder.Default
    private IEnumWSStatus.Types chatStatus = IEnumWSStatus.Types.DISCONNECTED;

    /**
     * Gets authorities.
     *
     * @param account the account
     * @return the authorities
     */
    public static Collection<? extends GrantedAuthority> getAuthorities(Account account) {
        if (Objects.isNull(account.getRoleInfo())) {
            return Collections.EMPTY_LIST;
        }

        return account.getRoleInfo().stream()
                .flatMap(roleInfo -> roleInfo.getPermissions().stream()
                        .map(ApiPermission::getRole)
                        .map(SimpleGrantedAuthority::new)).distinct()
                .toList();
    }

    /**
     * Gets password.
     *
     * @return the password
     */
    public String getPassword() {
        return new StringBuilder(this.getCode().toLowerCase())
                .append("@").append(this.getDomain())
                .append("@").append(this.getAuthType())
                .toString();
    }

    /**
     * Gets full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return getAccountDetails().getFullName();
    }

    public String getImagePath() {
        if (!StringUtils.hasText(this.imagePath)) {
            return "defaultPhoto.jpg";
        }
        return this.imagePath;
    }

    /**
     * Gets last connection date.
     *
     * @return the last connection date
     */
    public Date getLastConnectionDate() {
        if (CollectionUtils.isEmpty(this.getConnectionTracking())) {
            return null;
        }
        return this.getConnectionTracking().get(0).getLoginDate();
    }
}
