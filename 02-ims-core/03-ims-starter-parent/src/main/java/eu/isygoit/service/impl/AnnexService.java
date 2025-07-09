package eu.isygoit.service.impl;

import eu.isygoit.annotation.InjectRepository;
import eu.isygoit.com.rest.service.tenancy.CrudTenantService;
import eu.isygoit.model.Annex;
import eu.isygoit.repository.AnnexRepository;
import eu.isygoit.service.IAnnexService;
import jakarta.ws.rs.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The type Annex service.
 */
@Service
@Transactional
@InjectRepository(value = AnnexRepository.class)
public class AnnexService extends CrudTenantService<Long, Annex, AnnexRepository> implements IAnnexService {

    @Override
    public List<Annex> findAnnexByTableCode(String code) {
        return repository().findByTableCode(code);
    }

    @Override
    public List<Annex> findAnnexByTableCodeAndRef(String code, String reference) {
        List<Annex> annexes = repository().findByTableCodeAndReference(code, reference);
        if (!annexes.isEmpty()) {
            return annexes;
        } else {
            throw new NotFoundException("Annex not found with code and reference: " + code + " " + reference);
        }
    }
}
