package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
import eu.isygoit.app.ApplicationContextService;
import eu.isygoit.com.rest.service.impl.CrudService;
import eu.isygoit.model.Theme;
import eu.isygoit.repository.ThemeRepository;
import eu.isygoit.service.IThemeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The type Theme service.
 */
@Slf4j
@Service
@Transactional
@SrvRepo(value = ThemeRepository.class)
public class ThemeService extends CrudService<Long, Theme, ThemeRepository> implements IThemeService {

    private final ApplicationContextService applicationContextService;
    @Override
    protected ApplicationContextService getApplicationContextServiceInstance() {
        return applicationContextService;
    }

    private final ThemeRepository themeRepository;

    @Autowired
    public ThemeService(ApplicationContextService applicationContextService, ThemeRepository themeRepository) {
        this.applicationContextService = applicationContextService;
        this.themeRepository = themeRepository;
    }

    @Override
    public Optional<Theme> findThemeByAccountCodeAndDomainCode(String accountCode, String domainCode) {
        return themeRepository.findByAccountCodeIgnoreCaseAndDomainCodeIgnoreCase(accountCode, domainCode);
    }

    @Override
    public Optional<Theme> updateTheme(Theme theme) {
        this.findThemeByAccountCodeAndDomainCode(theme.getAccountCode(), theme.getDomainCode())
                .ifPresent(th -> theme.setId(th.getId()));
        return Optional.ofNullable(saveOrUpdate(theme));
    }
}
