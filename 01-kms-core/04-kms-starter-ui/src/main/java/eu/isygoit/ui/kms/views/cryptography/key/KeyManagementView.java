package eu.isygoit.ui.kms.views.cryptography.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeysResponse;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.component.ManagementGridSupport;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.kms.views.cryptography.key.dialog.CreateKeyDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "kms/keys", layout = KmsMainLayout.class)
@PermitAll
public class KeyManagementView extends AbstractPaginatedView<KeyManagementView.KeyRow> {
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    private final ComboBox<KeyStatusOption> statusFilter = new ComboBox<>();
    private final Span totalCountLabel = new Span();
    private IEnumKeyStatus.Types currentStatus = null;

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.pageSize = 10;

        totalCountLabel.addClassName("management-total-count");

        statusFilter.setItems(
                new KeyStatusOption(I18n.t("kms.key.view.status.all"), null),
                new KeyStatusOption(KmsEnumTag.label(IEnumKeyStatus.Types.ENABLED, null), IEnumKeyStatus.Types.ENABLED),
                new KeyStatusOption(KmsEnumTag.label(IEnumKeyStatus.Types.DISABLED, null), IEnumKeyStatus.Types.DISABLED),
                new KeyStatusOption(KmsEnumTag.label(IEnumKeyStatus.Types.PENDING_DELETION, null), IEnumKeyStatus.Types.PENDING_DELETION)
        );
        statusFilter.setItemLabelGenerator(option -> option.label());
        statusFilter.setRenderer(new ComponentRenderer<>(option -> option.value() == null
                ? new Span(option.label())
                : KmsEnumTag.of(option.value(), null)));
        statusFilter.setValue(new KeyStatusOption(I18n.t("kms.key.view.status.all"), null));
        statusFilter.setLabel(I18n.t("kms.key.view.status.placeholder"));
        statusFilter.setTooltipText(I18n.t("kms.key.view.status.tooltip"));
        statusFilter.setWidth("160px");
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue() != null ? e.getValue().value() : null;
            loadPage(0);
        });

        initPaginatedView();
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    // ─── Data ──────────────────────────────────────────────────────

    @Override
    protected PaginatedResponseDto<KeyRow> fetchPage(int page, int size) {
        // The backend token of this endpoint is the 0-based page index (null = first page).
        ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(size, page == 0 ? null : String.valueOf(page));
        ListKeysResponse body = response.getBody();
        List<ListKeysResponse.KeyEntry> keyEntries = (body != null && body.getKeys() != null) ? body.getKeys() : new ArrayList<>();

        List<KeyRow> rows = new ArrayList<>();
        for (ListKeysResponse.KeyEntry entry : keyEntries) {
            try {
                ResponseEntity<DescribeKeyResponse> descResponse = kmsApiService.describeKey(entry.getKeyId());
                DescribeKeyResponse describe = descResponse.getBody();
                rows.add(new KeyRow(entry.getKeyId(), describe != null ? describe.getKeyMetadata() : null));
            } catch (Exception ex) {
                rows.add(new KeyRow(entry.getKeyId(), null));
            }
        }
        return PaginatedResponseDto.<KeyRow>builder()
                .content(rows)
                .totalPages((body != null && body.getTotalPages() != null) ? body.getTotalPages() : 0)
                .totalElements((body != null && body.getTotalElements() != null) ? body.getTotalElements() : 0L)
                .pageNumber(page)
                .pageSize(size)
                .build();
    }

    @Override
    protected Component renderItem(KeyRow row) {
        return new KeyCard(this, kmsApiService, objectMapper, row.keyId(), row.metadata());
    }

    @Override
    protected boolean matchesSearch(KeyRow row, String search) {
        if (search == null || search.isEmpty()) {
            return true;
        }
        String searchLower = search.toLowerCase();
        return row.aliasOrId().toLowerCase().contains(searchLower)
                || row.keyId().toLowerCase().contains(searchLower);
    }

    @Override
    protected boolean matchesAdditionalFilters(KeyRow row) {
        return currentStatus == null || row.status() == currentStatus;
    }

    @Override
    protected void onSearchChanged() {
        // Previous behaviour: a search change reloads from the first page.
        loadPage(0);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("kms.key.view.total.count", totalElements));
    }

    @Override
    protected void openCreateDialog() {
        new CreateKeyDialog(this, kmsApiService, objectMapper, () -> loadPage(0)).open();
    }

    @Override
    protected boolean configureGrid(Grid<KeyRow> grid) {
        grid.addColumn(KeyRow::aliasOrId)
                .setHeader(I18n.t("kms.key.table.name")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(KeyRow::keyId)
                .setHeader(I18n.t("kms.key.card.key.id")).setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(row -> row.status() != null ? row.status().name() : I18n.t("kms.key.card.status.unknown"))
                .setHeader(I18n.t("kms.key.view.status.label")).setAutoWidth(true);
        ManagementGridSupport.addCardActionsColumn(grid, row -> new KeyCard(
                this, kmsApiService, objectMapper, row.keyId(), row.metadata(), false));
        return true;
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        return List.of(statusFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        return List.of(totalCountLabel);
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
        return new String[]{"kms-keys-view"};
    }

    @Override
    protected String getTitleKey() {
        return "kms.key.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "kms.key.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "kms.key.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "kms.key.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "kms.key.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "kms.key.view.empty.description";
    }

    @Override
    protected String getLoadErrorKey() {
        return "kms.key.view.load.error";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.KEY;
    }

    public record KeyStatusOption(String label, IEnumKeyStatus.Types value) {
    }

    /**
     * One listed key: its id and (when describeKey succeeded) its metadata.
     */
    public record KeyRow(String keyId, DescribeKeyResponse.KeyMetadata metadata) {
        String aliasOrId() {
            return (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                    ? metadata.getKeyAlias() : keyId;
        }

        IEnumKeyStatus.Types status() {
            return metadata != null ? metadata.getKeyStatus() : null;
        }
    }
}
