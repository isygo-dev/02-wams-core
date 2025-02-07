package eu.isygoit.service.impl;

import eu.isygoit.model.AppNextCode;
import eu.isygoit.repository.AppNextCodeRepository;
import eu.isygoit.repository.NextCodeRepository;
import eu.isygoit.service.AbstractNextCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The type Next code service.
 */
@Service
@Transactional
public class NextCodeService extends AbstractNextCodeService<AppNextCode, Long> {

    private final AppNextCodeRepository nextCodeRepository;

    @Autowired
    public NextCodeService(AppNextCodeRepository nextCodeRepository) {
        this.nextCodeRepository = nextCodeRepository;
    }

    @Override
    public NextCodeRepository nextCodeRepository() {
        return nextCodeRepository;
    }
}
