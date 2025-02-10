package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.exception.ObjectNotFoundException;
import eu.isygoit.model.Annex;
import eu.isygoit.repository.AnnexRepository;
import eu.isygoit.service.IAnnexService;
import jakarta.ws.rs.NotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * The type Annex service.
 */
@Service
@Transactional
@SrvRepo(value = AnnexRepository.class)
public class AnnexService extends CrudService<Long, Annex, AnnexRepository> implements IAnnexService {

    private final ApplicationContextService applicationContextService;

    public AnnexService(ApplicationContextService applicationContextService) {
        this.applicationContextService = applicationContextService;
    }

    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    @Override
    public List<Annex> getByTableCode(String code) {
        return repository().findByTableCode(code);
    }

    @Override
    public List<Annex> getByTableCodeAndRef(String code, String reference) {
        List<Annex> annexes = repository().findByTableCodeAndReference(code, reference);
        if (!annexes.isEmpty()) {
            return annexes;
        } else {
            throw new NotFoundException("Annex not found with code and reference: " + code + " " + reference);
        }
    }
}
