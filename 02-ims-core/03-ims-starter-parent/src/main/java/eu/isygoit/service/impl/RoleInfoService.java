package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectCodeGenKms;
import eu.isygoit.annotation.InjectCodeGen;
import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.com.rest.service.CodeAssignableService;
import eu.isygoit.com.rest.service.tenancy.CodeAssignableTenantService;
import eu.isygoit.constants.TenantConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.RoleInfo;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.AccountRepository;
import eu.isygoit.repository.AssoRoleInfoAccountRepository;
import eu.isygoit.repository.RoleInfoRepository;
import eu.isygoit.service.IRoleInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The type Role info service.
 */
@Service
@Transactional
@InjectCodeGen(value = NextCodeService.class)
@InjectCodeGenKms(value = KmsIncrementalKeyService.class)
@InjectRepository(value = RoleInfoRepository.class)
public class RoleInfoService extends CodeAssignableTenantService<Long, RoleInfo, RoleInfoRepository> implements IRoleInfoService {

    @Autowired
    private AssoRoleInfoAccountRepository assoRoleInfoAccountRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Override
    public AppNextCode initCodeGenerator() {
        return AppNextCode.builder()
                .tenant(TenantConstants.DEFAULT_TENANT_NAME)
                .entity(RoleInfo.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("RLE")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build();
    }

    @Override
    public RoleInfo findByName(String name) {
        return repository().findByName(name).orElse(null);
    }

    @Override
    public RoleInfo findByCodeIgnoreCase(String code) {
        return repository().findByCodeIgnoreCase(code).orElse(null);
    }

    @Override
    public List<RoleInfo> afterFindAll(String tenant, List<RoleInfo> list) {
        if (!CollectionUtils.isEmpty(list)) {
            list.stream().forEach(roleInfo -> {
                roleInfo.setNumberOfUsers(assoRoleInfoAccountRepository.countAllById_RoleInfoCode(roleInfo.getCode()));
            });
        }
        return super.afterFindAll(tenant,list);
    }

    @Override
    public RoleInfo afterFindById(String tenant, RoleInfo roleInfo) {
        roleInfo.setNumberOfUsers(assoRoleInfoAccountRepository.countAllById_RoleInfoCode(roleInfo.getCode()));
        return super.afterFindById(tenant, roleInfo);
    }

    /**
     * Filter not allowed roles list.
     *
     * @param requestContext the request context
     * @param list           the list
     * @return the list
     */
    public List<RoleInfoDto> filterNotAllowedRoles(RequestContextDto requestContext, List<RoleInfoDto> list) {
        Optional<Account> optional = accountRepository.findByTenantIgnoreCaseAndCodeIgnoreCase(requestContext.getSenderTenant(), requestContext.getSenderUser());
        if (optional.isPresent()) {
            RoleInfo senderRole = optional.get().getRoleInfo().parallelStream()
                    .min(Comparator.comparing(RoleInfo::getLevel))
                    .get();
            List<RoleInfoDto> filtered = list.stream().filter(roleInfo -> roleInfo.getLevel() >= senderRole.getLevel()).toList();
            return filtered;
        }

        throw new AccountNotFoundException("tenant " + requestContext.getSenderTenant() + " user " + requestContext.getSenderUser());
    }
}