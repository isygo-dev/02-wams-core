package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
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

/**
 * The type Annex service.
 */
@Service
@Transactional
@SrvRepo(value = AnnexRepository.class)
public class AnnexService extends CrudService<Long, Annex, AnnexRepository> implements IAnnexService {

    //@CachePut(cacheNames = SchemaTableConstantName.T_ANNEX, key = "{#annex.id}")
    @Override
    public Annex create(Annex annex) {
        return super.create(annex);
    }

    //@CachePut(cacheNames = SchemaTableConstantName.T_ANNEX, key = "{#annex.id}")
    @Override
    public Annex update(Annex annex) {
        return super.update(annex);
    }

    //@Cacheable(cacheNames = SchemaTableConstantName.T_ANNEX)
    @Override
    public List<Annex> findAll(Pageable pageable) {
        return super.findAll(pageable);
    }

    //@Cacheable(cacheNames = SchemaTableConstantName.T_ANNEX, key = "{#annex.id}")
    @Override
    public Annex findById(Long id) throws ObjectNotFoundException {
        return super.findById(id);
    }

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
