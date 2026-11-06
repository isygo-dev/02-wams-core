package eu.isygoit.ui.mms.views.sender;

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
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.sender.dialog.CreateSenderConfigDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "mms/sender-config", layout = MmsMainLayout.class)
@PermitAll
public class SenderConfigManagementView extends AbstractPaginatedView<SenderConfigDto> {
    private final SenderConfigService senderConfigService;
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final ComboBox<SenderStatusOption> statusFilter = new ComboBox<>();
    private Boolean currentActiveFilter = null;

    @Autowired
    public SenderConfigManagementView(SenderConfigService senderConfigService) {
        this.senderConfigService = senderConfigService;
        this.pageSize = 10;
        initPaginatedView();
        createButton.setTooltipText(I18n.t("mms.sender.view.create.tooltip"));
        searchField.setTooltipText(I18n.t("mms.sender.view.search.tooltip"));
    }

    @Override
    public String getPageTitle() {
        return I18n.t("mms.sender.view.title");
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    /**
     * The backend returns the full list: filters (search, status) are applied over the whole
     * list and the result is paged manually, exactly as before.
     */
    @Override
    protected PaginatedResponseDto<SenderConfigDto> fetchPage(int page, int size) {
        ResponseEntity<List<SenderConfigDto>> response = senderConfigService.findAllList();
        List<SenderConfigDto> all = response.getBody() != null ? response.getBody() : new ArrayList<>();
        List<SenderConfigDto> filtered = filterConfigs(all);

        int total = filtered.size();
        int totalPagesCount = (int) Math.ceil((double) total / size);
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, total);
        List<SenderConfigDto> pageContent = startIndex < total
                ? new ArrayList<>(filtered.subList(startIndex, endIndex))
                : new ArrayList<>();

        return PaginatedResponseDto.<SenderConfigDto>builder()
                .content(pageContent)
                .totalElements(total)
                .totalPages(totalPagesCount)
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    private List<SenderConfigDto> filterConfigs(List<SenderConfigDto> configs) {
        String search = searchField.getValue();
        return configs.stream()
                .filter(config -> {
                    if (currentActiveFilter != null) {
                        boolean isActive = config.getSmtpStarttlsEnable() != null && config.getSmtpStarttlsEnable();
                        if (isActive != currentActiveFilter) return false;
                    }
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        String code = config.getCode() != null ? config.getCode().toLowerCase() : "";
                        String name = config.getName() != null ? config.getName().toLowerCase() : "";
                        String host = config.getHost() != null ? config.getHost().toLowerCase() : "";
                        String username = config.getUsername() != null ? config.getUsername().toLowerCase() : "";
                        String tenant = config.getTenant() != null ? config.getTenant().toLowerCase() : "";
                        return code.contains(searchLower) ||
                                name.contains(searchLower) ||
                                host.contains(searchLower) ||
                                username.contains(searchLower) ||
                                tenant.contains(searchLower);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Search is already applied over the whole list in {@link #fetchPage(int, int)}.
     */
    @Override
    protected boolean matchesSearch(SenderConfigDto item, String search) {
        return true;
    }

    @Override
    protected void onSearchChanged() {
        loadPage(0);
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    @Override
    protected Component renderItem(SenderConfigDto item) {
        return new SenderConfigCard(this, senderConfigService, item, this::reloadFirstPage);
    }

    @Override
    protected boolean configureGrid(Grid<SenderConfigDto> grid) {
        grid.addColumn(SenderConfigDto::getCode)
                .setHeader(I18n.t("mms.sender.card.code")).setAutoWidth(true);
        grid.addColumn(SenderConfigDto::getName)
                .setHeader(I18n.t("mms.sender.card.name")).setAutoWidth(true);
        grid.addColumn(SenderConfigDto::getHost)
                .setHeader(I18n.t("mms.sender.card.host")).setAutoWidth(true);
        grid.addColumn(c -> I18n.t(c.getSmtpStarttlsEnable() != null && c.getSmtpStarttlsEnable()
                        ? "mms.sender.card.status.active" : "mms.sender.card.status.inactive"))
                .setHeader(I18n.t("mms.sender.view.status.label")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected void openCreateDialog() {
        new CreateSenderConfigDialog(this, senderConfigService, this::reloadFirstPage).open();
    }

    private void reloadFirstPage() {
        loadPage(0);
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────

    @Override
    protected List<Component> buildToolbarFilters() {
        statusFilter.setLabel(I18n.t("mms.sender.view.status.label"));
        statusFilter.setItems(
                new SenderStatusOption(I18n.t("mms.sender.view.status.all"), null),
                new SenderStatusOption(I18n.t("mms.sender.view.status.active"), true),
                new SenderStatusOption(I18n.t("mms.sender.view.status.inactive"), false)
        );
        statusFilter.setItemLabelGenerator(option -> option.label());
        statusFilter.setValue(new SenderStatusOption(I18n.t("mms.sender.view.status.all"), null));
        statusFilter.setPlaceholder(I18n.t("mms.sender.view.status.placeholder"));
        statusFilter.setTooltipText(I18n.t("mms.sender.view.status.tooltip"));
        statusFilter.setWidth("160px");
        statusFilter.addValueChangeListener(e -> {
            SenderStatusOption option = e.getValue();
            currentActiveFilter = option != null ? option.value() : null;
            loadPage(0);
        });
        return List.of(statusFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("mms.sender.view.refresh.tooltip"));
        refreshButton.addClickListener(e -> loadPage(0));
        return List.of(refreshButton);
    }

    // ─── Shell hooks ─────────────────────────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"sender-config-view"};
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException ex) {
            return (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        }
        return e.getMessage();
    }

    @Override
    protected String getLoadErrorKey() {
        return "mms.sender.view.load.error";
    }

    @Override
    protected String getTitleKey() {
        return "mms.sender.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "mms.sender.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "mms.sender.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "mms.sender.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "mms.sender.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "mms.sender.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.INBOX;
    }

    public record SenderStatusOption(String label, Boolean value) {
    }
}
