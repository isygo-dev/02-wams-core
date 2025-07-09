package eu.isygoit.security;

import eu.isygoit.dto.request.IsPwdExpiredRequestDto;
import eu.isygoit.enums.IEnumAccountSystemStatus;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.model.Account;
import eu.isygoit.model.Domain;
import eu.isygoit.remote.kms.KmsPasswordService;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type User service.
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private KmsPasswordService kmsPasswordService;
    @Autowired
    private DomainRepository tenantRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] userNameArray = username.split("@");
        if (userNameArray.length >= 2) {
            Domain tenant = tenantRepository.findByNameIgnoreCase(userNameArray[1]).orElseThrow(() -> new UsernameNotFoundException("User Not Found!"));
            Account account = accountRepository.findByTenantIgnoreCaseAndCodeIgnoreCase(userNameArray[1], userNameArray[0]).orElseThrow(() -> new UsernameNotFoundException("User Not Found!"));
            return CustomUserDetails.builder()
                    .username(account.getCode())
                    .isAdmin(account.getIsAdmin())
                    .password(username)
                    .passwordExpired(kmsPasswordService.isPasswordExpired(//RequestContextDto.builder().build(),
                            IsPwdExpiredRequestDto.builder()
                                    .tenant(account.getTenant())
                                    .email(account.getEmail())
                                    .userName(account.getCode())
                                    .authType(IEnumAuth.Types.valueOf(userNameArray[2]))
                                    .build()).getBody())
                    .authorities(Account.getAuthorities(account))
                    .tenantEnabled(tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED)
                    .accountEnabled(account.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED)
                    .accountExpired(account.getSystemStatus() == IEnumAccountSystemStatus.Types.EXPIRED)
                    .accountLocked(account.getSystemStatus() == IEnumAccountSystemStatus.Types.LOCKED
                            || account.getSystemStatus() == IEnumAccountSystemStatus.Types.TEM_LOCKED)
                    .build();
        }

        throw new UsernameNotFoundException(username);
    }
}
