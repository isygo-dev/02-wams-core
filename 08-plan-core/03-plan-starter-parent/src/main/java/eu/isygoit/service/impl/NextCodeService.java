package eu.isygoit.service.impl;

import eu.isygoit.model.AppNextCode;
import eu.isygoit.repository.AppNextCodeRepository;
import eu.isygoit.repository.code.NextCodeRepository;
import eu.isygoit.service.AbstractCodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Local next-code generator of the plan service (table {@code app_next_code}); used through
 * {@code @InjectCodeGen} by the code assignable services such as {@link PlanJobService}.
 */
@Slf4j
@Service
@Transactional
public class NextCodeService extends AbstractCodeGeneratorService<AppNextCode> {

    @Autowired
    private AppNextCodeRepository nextCodeRepository;

    @Override
    public NextCodeRepository nextCodeRepository() {
        return nextCodeRepository;
    }
}
