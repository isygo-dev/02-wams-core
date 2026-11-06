package eu.isygoit.ui.kms.views.cryptography.keyStore;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.KmsDtos.DescribeCustomKeyStoreResponse;
import eu.isygoit.dto.KmsDtos.ListCustomKeyStoresResponse;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyStore.dialog.CreateCustomKeyStoreDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@SpringComponent
@RouteScope
@Route(value = "kms/custom-key-stores", layout = KmsMainLayout.class)
@PermitAll
public class CustomKeyStoresView extends AbstractPaginatedView<DescribeCustomKeyStoreResponse.CustomKeyStore> {
    private final KmsApiService kmsApiService;
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final Span totalCountLabel = new Span();
    /**
     * The backend uses opaque cursor tokens (not page indexes): pageTokens.get(p) is the token that
     * returns page p (index 0 = null, the first page). Tokens are learnt while paging forward.
     */
    private final List<String> pageTokens = new ArrayList<>();

    @Autowired
    public CustomKeyStoresView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        this.pageSize = 10;

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> loadStores());
        refreshButton.setTooltipText(I18n.t("kms.keystore.view.refresh.stores"));
        totalCountLabel.addClassName("management-total-count");

        initPaginatedView();
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    /**
     * Reloads from the first page (used by the cards and dialogs after a change).
     */
    public void loadStores() {
        loadPage(0);
    }

    // ─── Data ──────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<DescribeCustomKeyStoreResponse.CustomKeyStore> fetchPage(int page, int size) {
        if (page <= 0 || pageTokens.isEmpty()) {
            pageTokens.clear();
            pageTokens.add(null);
        }
        int index = Math.min(Math.max(page, 0), pageTokens.size() - 1);
        ListCustomKeyStoresResponse body;
        // Walk forward from the furthest known token until the requested page (or the last page) is reached.
        while (true) {
            ResponseEntity<ListCustomKeyStoresResponse> response =
                    kmsApiService.listCustomKeyStores(size, pageTokens.get(index));
            body = response.getBody();
            String next = body != null ? body.getNextToken() : null;
            if (next != null) {
                if (pageTokens.size() > index + 1) {
                    pageTokens.set(index + 1, next);
                } else {
                    pageTokens.add(next);
                }
            }
            if (index >= page || next == null) {
                break;
            }
            index++;
        }

        List<DescribeCustomKeyStoreResponse.CustomKeyStore> stores = (body != null && body.getCustomKeyStores() != null)
                ? body.getCustomKeyStores() : new ArrayList<>();
        return PaginatedResponseDto.<DescribeCustomKeyStoreResponse.CustomKeyStore>builder()
                .content(stores)
                .totalPages((body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0)
                .totalElements((body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L)
                .pageNumber(index)
                .pageSize(size)
                .build();
    }

    @Override
    protected Component renderItem(DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        return new StoreCard(this, kmsApiService, store);
    }

    @Override
    protected boolean matchesSearch(DescribeCustomKeyStoreResponse.CustomKeyStore store, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String lower = search.toLowerCase();
        boolean nameMatch = store.getName() != null && store.getName().toLowerCase().contains(lower);
        boolean typeMatch = store.getCustomKeyStoreType() != null
                && store.getCustomKeyStoreType().toLowerCase().contains(lower);
        return nameMatch || typeMatch;
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.keystore.view.total.count", totalElements));
    }

    @Override
    protected void openCreateDialog() {
        new CreateCustomKeyStoreDialog(this, kmsApiService, this::loadStores).open();
    }

    @Override
    protected boolean configureGrid(Grid<DescribeCustomKeyStoreResponse.CustomKeyStore> grid) {
        grid.addColumn(DescribeCustomKeyStoreResponse.CustomKeyStore::getName)
                .setHeader(I18n.t("kms.keystore.table.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(DescribeCustomKeyStoreResponse.CustomKeyStore::getCustomKeyStoreType)
                .setHeader(I18n.t("kms.keystore.table.type")).setAutoWidth(true);
        grid.addColumn(store -> store.getStatus() != null ? store.getStatus().name() : "UNKNOWN")
                .setHeader(I18n.t("kms.keystore.table.status")).setAutoWidth(true);
        grid.addColumn(DescribeCustomKeyStoreResponse.CustomKeyStore::getConnectionState)
                .setHeader(I18n.t("kms.keystore.table.connection")).setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected List<Component> buildToolbarActions() {
        return List.of(refreshButton, totalCountLabel);
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        if (e instanceof FeignException ex) {
            return (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        }
        return e.getMessage();
    }

    // ─── Base-class contract ───────────────────────────────────────

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"kms-custom-stores-view"};
    }

    @Override
    protected String getTitleKey() {
        return "kms.keystore.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.keystore.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.keystore.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.keystore.view.filter.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.keystore.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.keystore.view.empty.description";
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.keystore.view.load.error";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.STORAGE;
    }
}
