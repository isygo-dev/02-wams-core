package eu.isygoit.ui.kms.views.tokenizer.config;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.tokenizer.config.dialog.CreateTokenConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "kms/token-configs", layout = KmsMainLayout.class)
@PermitAll
public class TokenConfigView extends AbstractPaginatedView<TokenConfigDto> {

    private final KmsTokenConfigService tokenConfigService;
    private final KmsApiService kmsApiService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();

    @Autowired
    public TokenConfigView(KmsTokenConfigService tokenConfigService, KmsApiService kmsApiService) {
        this.tokenConfigService = tokenConfigService;
        this.kmsApiService = kmsApiService;
        pageSize = 10;
        initPaginatedView();
        refreshButton.setTooltipText(I18n.t("tooltip.refresh"));
    }

    @Override
    protected PaginatedResponseDto<TokenConfigDto> fetchPage(int page, int size) {
        String searchCriteria = searchCriteria(searchField.getValue());
        var response = searchCriteria.isBlank()
                ? tokenConfigService.findAll(page, size)
                : tokenConfigService.findAllFilteredByCriteria(searchCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends TokenConfigDto> fetchFilteredPage(
            String criteria, int page, int size) {
        String mergedCriteria = mergeCriteria(criteria, searchCriteria(searchField.getValue()));
        var response = tokenConfigService.findAllFilteredByCriteria(mergedCriteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = tokenConfigService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<TokenConfigDto> grid) {
        grid.addColumn(TokenConfigDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(TokenConfigDto::getTokenType).setHeader("Token type").setAutoWidth(true);
        grid.addColumn(TokenConfigDto::getLifeTimeInMs).setHeader("Lifetime (ms)").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(TokenConfigDto item) {
        return new TokenConfigCard(this, tokenConfigService, kmsApiService, item, this::loadConfigs);
    }

    @Override
    protected boolean matchesSearch(TokenConfigDto item, String search) {
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreateTokenConfigDialog(tokenConfigService, kmsApiService, this::loadConfigs).open();
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
        totalCountLabel.setText(I18n.t("kms.token.config.total.count", totalElements));
    }

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"token-config-view"};
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
        return "kms.token.config.header";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.token.config.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.token.config.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.token.config.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.token.config.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.token.config.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.CODE;
    }

    @Override
    protected String getLoadErrorKey() {
        return "notification.error.load";
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

    public void refreshCard(TokenConfigCard card) {
        try {
            var response = tokenConfigService.findById(card.getDto().getId());
            TokenConfigDto updated = response.getBody();
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
                    I18n.t("notification.error.load") + ": " + exception.getMessage());
        }
    }

    private String searchCriteria(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        String escaped = search.replace("'", "''");
        return "(code ~ '" + escaped + "' | tokenType ~ '" + escaped + "')";
    }

    private String mergeCriteria(String criteria, String search) {
        if (search.isBlank()) {
            return criteria;
        }
        return criteria == null || criteria.isBlank() ? search : "(" + criteria + ") & " + search;
    }
}
