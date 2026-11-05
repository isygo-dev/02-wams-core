package eu.isygoit.ui.kms.views.secrets.digest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.secrets.digest.dialog.CreateDigestConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@VaadinSessionScope
@Route(value = "kms/digest-configs", layout = KmsMainLayout.class)
@PageTitle("Digest Configurations")
@PermitAll
public class DigestConfigView extends AbstractPaginatedView<DigestConfigDto> {

    private final DigestConfigService configService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();

    @Autowired
    public DigestConfigView(DigestConfigService configService) {
        this.configService = configService;
        pageSize = 10;
        initPaginatedView();
        refreshButton.setTooltipText(I18n.t("kms.digest.config.refresh.tooltip"));
    }

    @Override
    protected PaginatedResponseDto<DigestConfigDto> fetchPage(int page, int size) {
        String searchCriteria = searchCriteria(searchField.getValue());
        var response = searchCriteria.isBlank()
                ? configService.findAll(page, size)
                : configService.findAllFilteredByCriteria(searchCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends DigestConfigDto> fetchFilteredPage(
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
    protected boolean configureGrid(Grid<DigestConfigDto> grid) {
        grid.addColumn(DigestConfigDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(DigestConfigDto::getAlgorithm).setHeader("Algorithm").setAutoWidth(true);
        grid.addColumn(DigestConfigDto::getIterations).setHeader("Iterations").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(DigestConfigDto item) {
        return new DigestConfigCard(this, configService, item, this::loadConfigs);
    }

    @Override
    protected boolean matchesSearch(DigestConfigDto item, String search) {
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreateDigestConfigDialog(configService, this::loadConfigs).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(event -> loadPageZero());
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
        return List.of(refreshButton, pageSizeSelect, totalCountLabel);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.digest.config.total.count", totalElements));
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"digest-config-view"};
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
        return "kms.digest.config.header";
    }

    @Override
    protected String getSubtitleKey() {
        return null;
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.digest.config.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.digest.config.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.digest.config.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.digest.config.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.DIPLOMA;
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.digest.config.load.error";
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

    public void refreshCard(DigestConfigCard card) {
        try {
            var response = configService.findById(card.getDto().getId());
            DigestConfigDto updated = response.getBody();
            if (updated == null) {
                loadConfigs();
                return;
            }
            card.updateDto(updated);
            replaceCurrentItem(updated);
        } catch (Exception exception) {
            eu.isygoit.ui.common.notification.AppNotifications.error(
                    I18n.t("kms.digest.config.refresh.card.error", exception.getMessage()));
        }
    }

    private void replaceCurrentItem(DigestConfigDto updated) {
        for (int i = 0; i < currentItems.size(); i++) {
            if (currentItems.get(i).getId().equals(updated.getId())) {
                currentItems.set(i, updated);
                applyLocalFilter();
                return;
            }
        }
    }

    private String searchCriteria(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return "code ~ '" + escapeCriteriaValue(search) + "'";
    }

    private String mergeCriteria(String criteria, String search) {
        if (search.isBlank()) {
            return criteria;
        }
        return criteria == null || criteria.isBlank() ? search : "(" + criteria + ") & (" + search + ")";
    }

    private String escapeCriteriaValue(String value) {
        return value.replace("'", "''");
    }
}
