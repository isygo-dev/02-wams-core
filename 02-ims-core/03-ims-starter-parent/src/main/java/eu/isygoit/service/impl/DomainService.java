package eu.isygoit.service.impl;

import eu.isygoit.annotation.CodeGenKms;
import eu.isygoit.annotation.CodeGenLocal;
import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.com.rest.service.impl.ImageService;
import eu.isygoit.config.AppProperties;
import eu.isygoit.constants.DomainConstants;
import eu.isygoit.enums.IEnumBinaryStatus;
import eu.isygoit.exception.DomainNotFoundException;
import eu.isygoit.model.AppNextCode;
import eu.isygoit.model.Domain;
import eu.isygoit.model.extendable.NextCodeModel;
import eu.isygoit.model.schema.SchemaColumnConstantName;
import eu.isygoit.remote.kms.KmsIncrementalKeyService;
import eu.isygoit.repository.DomainRepository;
import eu.isygoit.service.IDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The type Domain service.
 */
@Service
@Transactional
@CodeGenLocal(value = NextCodeService.class)
@CodeGenKms(value = KmsIncrementalKeyService.class)
@SrvRepo(value = DomainRepository.class)
public class DomainService extends ImageService<Long, Domain, DomainRepository> implements IDomainService {

    private final AppProperties appProperties;


    /**
     * Instantiates a new Domain service.
     *
     * @param appProperties the app properties
     */
    public DomainService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public List<String> getAllDomainNames(String domain) {
        if (DomainConstants.SUPER_DOMAIN_NAME.equals(domain)) {
            return repository().findAllNames(); //.getAll().stream().map(domain -> domain.getName()).collect(Collectors.toUnmodifiableList());
        }

        return Arrays.asList(domain);
    }

    @Override
    public Domain updateAdminStatus(Long id, IEnumBinaryStatus.Types newStatus) {
        repository().updateAdminStatusById(id, newStatus);
        return repository().findById(id).orElse(null);
    }

    @Override
    public String getImage(String domainName) {
        Optional<Domain> domainOptional = repository().findByNameIgnoreCase(domainName);
        if (domainOptional.isPresent()) {
            return domainOptional.get().getImagePath();
        }
        return "";
    }

    @Override
    public Optional<Long> findDomainIdbyDomainName(String name) {
        Optional<Domain> optional = repository().findByNameIgnoreCase(name);
        if (optional.isPresent()) {
            return Optional.ofNullable(optional.get().getId());
        }
        return Optional.empty();
    }

    @Override
    public Optional<Domain> findByName(String name) {
        return repository().findByNameIgnoreCase(name);
    }

    @Override
    public boolean isEnabled(String domain) {
        return repository().getAdminStatus(domain) == IEnumBinaryStatus.Types.ENABLED;
    }

    @Override
    public Domain updateSocialLink(String senderDomain, Long id, String social, String link) {
        Optional<Domain> optional = repository().findById(id);
        if (!optional.isPresent()) {
            throw new DomainNotFoundException("with id " + id);
        }

        Domain domain = optional.get();
        switch (social) {
            case "lnk_facebook": {
                domain.setLnk_facebook(link);
            }
            break;
            case "lnk_linkedin": {
                domain.setLnk_linkedin(link);
            }
            break;
            case "lnk_xing": {
                domain.setLnk_xing(link);
            }
            break;
        }

        return this.update(domain);
    }

    @Override
    public Optional<NextCodeModel> initCodeGenerator() {
        return Optional.ofNullable(AppNextCode.builder()
                .domain(DomainConstants.DEFAULT_DOMAIN_NAME)
                .entity(Domain.class.getSimpleName())
                .attribute(SchemaColumnConstantName.C_CODE)
                .prefix("DOM")
                .valueLength(6L)
                .value(1L)
                .increment(1)
                .build());
    }

    @Override
    protected String getUploadDirectory() {
        return this.appProperties.getUploadDirectory();
    }
}
