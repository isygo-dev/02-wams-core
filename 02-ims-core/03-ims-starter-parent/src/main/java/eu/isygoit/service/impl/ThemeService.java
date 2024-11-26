package eu.isygoit.service.impl;

import eu.isygoit.annotation.SrvRepo;
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

    @Autowired
    private ThemeRepository themeRepository;

    @Override
    public Theme findThemeByAccountCodeAndDomainCode(String accountCode, String domainCode) {
        Optional<Theme> theme = themeRepository.findByAccountCodeIgnoreCaseAndDomainCodeIgnoreCase(accountCode, domainCode);
        if (theme.isPresent()) {
            return theme.get();
        }
        return null;
    }

    @Override
    public Theme updateTheme(Theme theme) {
        Theme oldTheme = this.findThemeByAccountCodeAndDomainCode(theme.getAccountCode(), theme.getDomainCode());
        if (oldTheme != null) {
            theme.setId(oldTheme.getId());
        }
        return saveOrUpdate(theme);
    }
}
