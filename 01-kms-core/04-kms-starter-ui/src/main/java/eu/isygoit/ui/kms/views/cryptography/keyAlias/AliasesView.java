package eu.isygoit.ui.kms.views.cryptography.keyAlias;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.KmsDtos.ListAliasesResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.CreateAliasDialog;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "kms/aliases", layout = KmsMainLayout.class)
@PermitAll
public class AliasesView extends AbstractPaginatedView<ListAliasesResponse.AliasEntry> {
    private final KmsApiService kmsApiService;
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Span totalCountLabel = new Span();

    @Autowired
    public AliasesView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        this.pageSize = 10;

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> resetPaginationAndLoad());
        refreshButton.setTooltipText(I18n.t("kms.aliases.view.refresh.tooltip"));
        totalCountLabel.addClassName("management-total-count");
        totalCountLabel.getElement().setAttribute("title", I18n.t("kms.aliases.view.total.count.title"));

        initPaginatedView();
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    /**
     * Reloads from the first page (used by the cards and dialogs after a change).
     */
    void resetPaginationAndLoad() {
        loadPage(0);
    }

    // ─── Data ──────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<ListAliasesResponse.AliasEntry> fetchPage(int page, int size) {
        // The backend token of this endpoint is the 0-based page index (null = first page).
        ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(size, page == 0 ? null : String.valueOf(page));
        ListAliasesResponse body = response.getBody();
        List<ListAliasesResponse.AliasEntry> aliasEntries = (body != null && body.getAliases() != null)
                ? body.getAliases() : new ArrayList<>();
        return PaginatedResponseDto.<ListAliasesResponse.AliasEntry>builder()
                .content(aliasEntries)
                .totalPages((body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0)
                .totalElements((body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L)
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    @Override
    protected Component renderItem(ListAliasesResponse.AliasEntry entry) {
        return new AliasCard(this, kmsApiService, entry);
    }

    @Override
    protected boolean matchesSearch(ListAliasesResponse.AliasEntry entry, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        String searchLower = search.toLowerCase();
        return (entry.getAliasName() != null && entry.getAliasName().toLowerCase().contains(searchLower))
                || (entry.getTargetKeyId() != null && entry.getTargetKeyId().toLowerCase().contains(searchLower));
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.aliases.view.total.count", totalElements));
    }

    @Override
    protected void openCreateDialog() {
        new CreateAliasDialog(this, kmsApiService, this::resetPaginationAndLoad).open();
    }

    @Override
    protected boolean configureGrid(Grid<ListAliasesResponse.AliasEntry> grid) {
        grid.addColumn(ListAliasesResponse.AliasEntry::getAliasName)
                .setHeader(I18n.t("kms.alias.table.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(ListAliasesResponse.AliasEntry::getTargetKeyId)
                .setHeader(I18n.t("kms.alias.card.target.key")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(ListAliasesResponse.AliasEntry::getCreateDate)
                .setHeader(I18n.t("kms.alias.card.created")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected List<Component> buildToolbarActions() {
        return List.of(refreshButton, totalCountLabel);
    }

    public List<String> fetchKeyIds() {
        List<String> keyIds = new ArrayList<>();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyIds = keys.getKeys().stream()
                        .map(ListKeysResponse.KeyEntry::getKeyId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Notification.show(I18n.t("kms.aliases.view.load.keys.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }

    // ─── Base-class contract ───────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"kms-aliases-view"};
    }

    @Override
    protected String getTitleKey() {
        return "kms.aliases.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.aliases.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.aliases.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.aliases.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.aliases.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.aliases.view.empty.description";
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.aliases.view.load.error";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.TAG;
    }
}
