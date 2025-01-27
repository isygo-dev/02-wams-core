package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.CodifiableService;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.dto.common.RequestContextDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.exception.AccountNotFoundException;
import eu.isygoit.model.Account;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.RoleInfo;
import eu.isygoit.model.extendable.NextCodeModel;
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
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = RoleInfoRepository.class)
public class RoleInfoService extends CodifiableService<Long, RoleInfo, RoleInfoRepository> implements IRoleInfoService {

    @Autowired
    private AssoRoleInfoAccountRepository assoRoleInfoAccountRepository;
    @Autowired
    private AccountRepository accountRepository;

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(RoleInfo.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("RLE")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }

    @Override
    public Optional<RoleInfo> findByName(String name) {
        return repository().findByName(name);
    }

    @Override
    public Optional<RoleInfo> findByCodeIgnoreCase(String code) {
        return repository().findByCodeIgnoreCase(code);
    }

    @Override
    public List<RoleInfo> afterFindAll(List<RoleInfo> list) {
        if (!CollectionUtils.isEmpty(list)) {
            list.stream().forEach(roleInfo -> {
                roleInfo.setNumberOfUsers(assoRoleInfoAccountRepository.countAllById_RoleInfoCode(roleInfo.getCode()));
            });
        }
        return super.afterFindAll(list);
    }

    @Override
    public RoleInfo afterFindById(RoleInfo roleInfo) {
        roleInfo.setNumberOfUsers(assoRoleInfoAccountRepository.countAllById_RoleInfoCode(roleInfo.getCode()));
        return super.afterFindById(roleInfo);
    }

    /**
     * Filter not allowed roles list.
     *
     * @param requestContext the request context
     * @param list           the list
     * @return the list
     */
    public List<RoleInfoDto> filterNotAllowedRoles(RequestContextDto requestContext, List<RoleInfoDto> list) {
        Optional<Account> optional = accountRepository.findByDomainIgnoreCaseAndCodeIgnoreCase(requestContext.getSenderDomain(), requestContext.getSenderUser());
        if (optional.isPresent()) {
            RoleInfo senderRole = optional.get().getRoleInfo().parallelStream()
                    .min(Comparator.comparing(RoleInfo::getLevel))
                    .get();
            List<RoleInfoDto> filtered = list.stream().filter(roleInfo -> roleInfo.getLevel() >= senderRole.getLevel()).toList();
            return filtered;
        }

        throw new AccountNotFoundException("domain " + requestContext.getSenderDomain() + " user " + requestContext.getSenderUser());
    }
}