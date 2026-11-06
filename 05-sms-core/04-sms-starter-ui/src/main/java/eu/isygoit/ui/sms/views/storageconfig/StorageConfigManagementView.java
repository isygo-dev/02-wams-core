package eu.isygoit.ui.sms.views.storageconfig;

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
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.enums.IEnumStorage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.sms.layout.SmsMainLayout;
import eu.isygoit.ui.sms.views.common.SmsEnumTag;
import eu.isygoit.ui.sms.views.storageconfig.dialog.CreateStorageConfigDialog;
import eu.isygoit.ui.sms.views.storageconfig.dialog.UpdateStorageConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "sms/storageconfigs", layout = SmsMainLayout.class)
@PermitAll
public class StorageConfigManagementView extends AbstractPaginatedView<StorageConfigDto> {

    private final StorageConfigService storageConfigService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final ComboBox<IEnumStorage.Types> typeFilter = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();
    private IEnumStorage.Types currentType;

    @Autowired
    public StorageConfigManagementView(StorageConfigService storageConfigService) {
        this.storageConfigService = storageConfigService;
        pageSize = 10;
        initPaginatedView();
        refreshButton.setTooltipText(I18n.t("sms.storageconfig.view.refresh.tooltip"));
    }

    @Override
    protected PaginatedResponseDto<StorageConfigDto> fetchPage(int page, int size) {
        var response = storageConfigService.findAll(page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends StorageConfigDto> fetchFilteredPage(
            String criteria, int page, int size) {
        var response = storageConfigService.findAllFilteredByCriteria(criteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = storageConfigService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<StorageConfigDto> grid) {
        grid.addColumn(StorageConfigDto::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(StorageConfigDto::getTenant).setHeader("Tenant").setFlexGrow(1);
        grid.addColumn(StorageConfigDto::getType).setHeader("Type").setAutoWidth(true);
        grid.addColumn(StorageConfigDto::getUserName).setHeader("User").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(StorageConfigDto item) {
        return new StorageConfigCard(this, storageConfigService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(StorageConfigDto config, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase();
        return (config.getUserName() != null && config.getUserName().toLowerCase().contains(query))
                || (config.getUrl() != null && config.getUrl().toLowerCase().contains(query))
                || (config.getTenant() != null && config.getTenant().toLowerCase().contains(query));
    }

    @Override
    protected boolean matchesAdditionalFilters(StorageConfigDto config) {
        return currentType == null || config.getType() == currentType;
    }

    @Override
    protected void openCreateDialog() {
        new CreateStorageConfigDialog(this, storageConfigService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        typeFilter.setItems(IEnumStorage.Types.values());
        SmsEnumTag.useTagRenderer(typeFilter, "sms.enum.storage");
        typeFilter.setPlaceholder(I18n.t("sms.storageconfig.view.type.placeholder"));
        typeFilter.setWidth("150px");
        typeFilter.addValueChangeListener(event -> {
            currentType = event.getValue();
            loadPageZero();
        });
        return List.of(typeFilter);
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
        totalCountLabel.setText(I18n.t("sms.storageconfig.view.total.count", totalElements));
    }

    @Override
    protected String extractErrorMessage(Exception exception) {
        if (exception instanceof FeignException feignException
                && feignException.contentUTF8() != null && !feignException.contentUTF8().isBlank()) {
            return feignException.contentUTF8();
        }
        return exception.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "sms.storageconfig.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return null;
    }

    @Override
    protected String getCreateButtonKey() {
        return "sms.storageconfig.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "sms.storageconfig.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "sms.storageconfig.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "sms.storageconfig.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.DATABASE;
    }

    @Override
    protected String getLoadErrorKey() {
        return "sms.storageconfig.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateStorageConfigDialog(StorageConfigDto config, Runnable onSuccess) {
        new UpdateStorageConfigDialog(this, storageConfigService, config, onSuccess).open();
    }
}
