package eu.isygoit.ui.kms.views.secrets.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.secrets.password.dialog.CreatePasswordConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "kms/password-configs", layout = KmsMainLayout.class)
@PermitAll
public class PasswordConfigView extends AbstractPaginatedView<PasswordConfigDto> {

    private final PasswordConfigService configService;
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();

    @Autowired
    public PasswordConfigView(PasswordConfigService configService) {
        this.configService = configService;
        pageSize = 10;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<PasswordConfigDto> fetchPage(int page, int size) {
        String searchCriteria = searchCriteria(searchField.getValue());
        var response = searchCriteria.isBlank()
                ? configService.findAll(page, size)
                : configService.findAllFilteredByCriteria(searchCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends PasswordConfigDto> fetchFilteredPage(
            String criteria, int page, int size) {
        String mergedCriteria = mergeCriteria(criteria, searchCriteria(searchField.getValue()));
        var response = configService.findAllFilteredByCriteria(mergedCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = configService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<PasswordConfigDto> grid) {
        grid.addColumn(PasswordConfigDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(PasswordConfigDto::getType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(PasswordConfigDto::getMinLength).setHeader("Minimum length").setAutoWidth(true);
        grid.addColumn(PasswordConfigDto::getMaxLength).setHeader("Maximum length").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(PasswordConfigDto item) {
        return new PasswordConfigCard(this, configService, item, this::loadConfigs);
    }

    @Override
    protected boolean matchesSearch(PasswordConfigDto item, String search) {
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreatePasswordConfigDialog(configService, this::loadConfigs).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarActions() {
        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setWidth("100px");
        pageSizeSelect.addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue() != pageSize) {
                pageSize = event.getValue();
                loadPageZero();
            }
        });
        totalCountLabel.addClassName("management-total-count");
        return List.of(pageSizeSelect, totalCountLabel);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.password.config.total.count", totalElements));
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"password-config-view"};
    }

    @Override
    protected String extractErrorMessage(Exception exception) {
        if (exception instanceof FeignException feignException
                && (feignException.status() == 400 || feignException.status() == 500)
                && feignException.contentUTF8() != null && !feignException.contentUTF8().isBlank()) {
            return feignException.contentUTF8();
        }
        return exception.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "kms.password.config.header";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.password.config.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.password.config.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.password.config.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.password.config.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.password.config.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.USER;
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.password.config.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    public void loadConfigs() {
        loadPage(currentPage);
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void refreshCard(PasswordConfigCard card) {
        try {
            var response = configService.findById(card.getDto().getId());
            PasswordConfigDto updated = response.getBody();
            if (updated == null) {
                loadConfigs();
                return;
            }
            card.updateDto(updated);
            for (int i = 0; i < currentItems.size(); i++) {
                if (currentItems.get(i).getId().equals(updated.getId())) {
                    currentItems.set(i, updated);
                    applyLocalFilter();
                    return;
                }
            }
        } catch (Exception exception) {
            eu.isygoit.ui.common.notification.AppNotifications.error(
                    I18n.t("kms.password.config.refresh.card.error", exception.getMessage()));
        }
    }

    private String searchCriteria(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return "code ~ '" + search.replace("'", "''") + "'";
    }

    private String mergeCriteria(String criteria, String search) {
        if (search.isBlank()) {
            return criteria;
        }
        return criteria == null || criteria.isBlank() ? search : "(" + criteria + ") & (" + search + ")";
    }
}
