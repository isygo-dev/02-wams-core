package eu.isygoit.ui.kms.views.cryptography.keyGrants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog.CreateGrantDialog;
import eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog.GrantDetailsViewDialog;
import eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog.RetireGrantDialog;
import eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog.RevokeGrantDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@VaadinSessionScope
@Route(value = "kms/grants", layout = KmsMainLayout.class)
@PageTitle("Grants")
@PermitAll
public class GrantsView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    // UI components
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>(I18n.t("kms.grants.view.select.key"));
    private final TextField filterField = new TextField();
    private final Button clearFilterButton = new Button(new Icon(VaadinIcon.CLOSE));
    private final Grid<KmsDtos.ListGrantsResponse.Grant> grantsGrid = new Grid<>();
    private final Button refreshButton = new Button(I18n.t("kms.grants.view.refresh.button"), new Icon(VaadinIcon.REFRESH));
    private final Button createGrantButton = new Button(I18n.t("kms.grants.view.create.grant.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button revokeGrantButton = new Button(I18n.t("kms.grants.view.revoke.button"), new Icon(VaadinIcon.BAN));
    private final Button retireGrantButton = new Button(I18n.t("kms.grants.view.retire.button"), new Icon(VaadinIcon.CLOSE_CIRCLE));
    private final Button viewDetailsButton = new Button(I18n.t("kms.grants.view.details.button"), new Icon(VaadinIcon.EYE));
    private final ProgressBar loadingBar = new ProgressBar();

    private String selectedKeyId = null;
    private List<KeyOption> keyOptions = new ArrayList<>();
    private List<KmsDtos.ListGrantsResponse.Grant> allGrants = new ArrayList<>();

    @Autowired
    public GrantsView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-grants-view");

        buildHeader();
        buildKeySelector();
        buildFilterBar();
        buildActionBar();
        buildGrantsGrid();
        buildLoadingIndicator();

        refreshButton.addClickListener(e -> loadGrants());
        createGrantButton.addClickListener(e -> openCreateGrantDialog());
        revokeGrantButton.addClickListener(e -> revokeSelectedGrant());
        retireGrantButton.addClickListener(e -> retireSelectedGrant());
        viewDetailsButton.addClickListener(e -> showGrantDetails());

        filterField.addValueChangeListener(e -> applyFilter());
        clearFilterButton.addClickListener(e -> {
            filterField.clear();
            applyFilter();
        });

        loadKeyOptions();
    }

    // ------------------------------------------------------------------------
    // UI Building
    // ------------------------------------------------------------------------

    private void buildHeader() {
        H2 header = new H2(I18n.t("kms.grants.view.title"));
        header.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.Bottom.NONE);
        add(header);
    }

    private void buildKeySelector() {
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.addClassName("grants-key-layout");

        keyCombo.setPlaceholder(I18n.t("kms.grants.view.select.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            selectedKeyId = e.getValue() != null ? e.getValue().getKeyId() : null;
            if (selectedKeyId != null) {
                loadGrants();
            } else {
                allGrants.clear();
                grantsGrid.setItems(new ArrayList<>());
            }
        });

        Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText(I18n.t("kms.grants.view.refresh.tooltip"));
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        add(keyLayout);
    }

    private void buildFilterBar() {
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        filterLayout.setSpacing(true);
        filterLayout.addClassName("grants-key-layout");

        filterField.setPlaceholder(I18n.t("kms.grants.view.filter.placeholder"));
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(300);
        filterField.setWidth("300px");
        filterField.setClearButtonVisible(true);

        clearFilterButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFilterButton.setTooltipText(I18n.t("kms.grants.view.clear.filter.tooltip"));
        clearFilterButton.setEnabled(false);

        filterLayout.add(filterField, clearFilterButton);
        add(filterLayout);
    }

    private void buildActionBar() {
        // Same conceptual order as card action bars: refresh/create (neutral/primary),
        // details/view (info-style), then destructive actions last, danger-styled and
        // rightmost (retire before revoke — revoke is the more severe/irreversible action).
        HorizontalLayout actionBar = new HorizontalLayout(refreshButton, createGrantButton,
                viewDetailsButton, retireGrantButton, revokeGrantButton);
        actionBar.setSpacing(true);
        actionBar.addClassName("grants-action-bar");

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClassName("wams-action-btn");
        refreshButton.setTooltipText(I18n.t("kms.grants.view.refresh.grants.tooltip"));
        createGrantButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createGrantButton.setTooltipText(I18n.t("kms.grants.view.create.grant.tooltip"));
        viewDetailsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        viewDetailsButton.addClassName("wams-action-btn");
        viewDetailsButton.setTooltipText(I18n.t("kms.grants.view.details.tooltip"));
        retireGrantButton.addThemeVariants(ButtonVariant.LUMO_WARNING);
        retireGrantButton.addClassName("wams-action-btn");
        retireGrantButton.addClassName("wams-action-btn--danger");
        retireGrantButton.setTooltipText(I18n.t("kms.grants.view.retire.grant.tooltip"));
        revokeGrantButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        revokeGrantButton.addClassName("wams-action-btn");
        revokeGrantButton.addClassName("wams-action-btn--danger");
        revokeGrantButton.setTooltipText(I18n.t("kms.grants.view.revoke.grant.tooltip"));

        add(actionBar);
    }

    private void buildGrantsGrid() {
        grantsGrid.setWidthFull();
        grantsGrid.setHeight("500px");
        grantsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COLUMN_BORDERS);
        grantsGrid.addClassName("grants-grid");

        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getGrantId)
                .setHeader(I18n.t("kms.grants.view.grid.column.grant.id")).setSortable(true).setFlexGrow(0).setWidth("200px");
        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getGranteePrincipal)
                .setHeader(I18n.t("kms.grants.view.grid.column.grantee")).setSortable(true);
        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getRetiringPrincipal)
                .setHeader(I18n.t("kms.grants.view.grid.column.retiring")).setSortable(true);
        grantsGrid.addColumn(grant -> grant.getOperations() != null ? String.join(", ", grant.getOperations()) : "[]")
                .setHeader(I18n.t("kms.grants.view.grid.column.operations")).setSortable(true);
        grantsGrid.addColumn(new ComponentRenderer<>(grant -> {
            String status = grant.getStatus() != null ? grant.getStatus() : "ACTIVE";
            Span chip = new Span(status);
            chip.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.Padding.Vertical.XSMALL, LumoUtility.BorderRadius.LARGE);
            chip.addClassName("wams-chip");
            String statusDisplay;
            switch (status.toUpperCase()) {
                case "ACTIVE":
                    chip.addClassName("wams-chip--success");
                    statusDisplay = I18n.t("kms.grants.view.status.active");
                    break;
                case "REVOKED":
                    chip.addClassName("wams-chip--error");
                    statusDisplay = I18n.t("kms.grants.view.status.revoked");
                    break;
                case "RETIRED":
                    chip.addClassName("wams-chip--neutral");
                    statusDisplay = I18n.t("kms.grants.view.status.retired");
                    break;
                default:
                    chip.addClassName("wams-chip--neutral");
                    statusDisplay = status;
            }
            chip.setText(statusDisplay);
            return chip;
        })).setHeader(I18n.t("kms.grants.view.grid.column.status")).setSortable(true);

        grantsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grantsGrid.addSelectionListener(selection -> {
            boolean hasSelection = selection.getFirstSelectedItem().isPresent();
            revokeGrantButton.setEnabled(hasSelection);
            retireGrantButton.setEnabled(hasSelection);
            viewDetailsButton.setEnabled(hasSelection);
        });

        add(grantsGrid);
    }

    private void buildLoadingIndicator() {
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);
    }

    // ------------------------------------------------------------------------
    // Data Loading
    // ------------------------------------------------------------------------

    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions.clear();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(k -> k.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                allGrants.clear();
                grantsGrid.setItems(new ArrayList<>());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.grants.view.load.keys.error", errorMsg));
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showError(I18n.t("kms.grants.view.load.keys.error", e.getMessage()));
            log.error("Failed to load keys: {}", e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && StringUtils.hasText(desc.getKeyMetadata().getKeyAlias())) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception ignored) {
        }
        return keyId;
    }

    private void loadGrants() {
        if (selectedKeyId == null) return;
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListGrantsResponse> response = kmsApiService.listGrants(selectedKeyId, 100, null, null, null);
            KmsDtos.ListGrantsResponse grants = response.getBody();
            if (grants != null && grants.getGrants() != null) {
                allGrants = grants.getGrants();
                applyFilter();
            } else {
                allGrants.clear();
                grantsGrid.setItems(new ArrayList<>());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.grants.view.load.grants.error", errorMsg));
            log.error("Failed to load grants for key {}: {}", selectedKeyId, errorMsg);
            allGrants.clear();
            grantsGrid.setItems(new ArrayList<>());
        } catch (Exception e) {
            showError(I18n.t("kms.grants.view.load.grants.error", e.getMessage()));
            log.error("Failed to load grants for key {}: {}", selectedKeyId, e.getMessage());
            allGrants.clear();
            grantsGrid.setItems(new ArrayList<>());
        } finally {
            showLoading(false);
        }
    }

    private void applyFilter() {
        String filter = filterField.getValue();
        clearFilterButton.setEnabled(StringUtils.hasText(filter));
        if (!StringUtils.hasText(filter)) {
            grantsGrid.setItems(allGrants);
        } else {
            List<KmsDtos.ListGrantsResponse.Grant> filtered = allGrants.stream()
                    .filter(g -> g.getGranteePrincipal() != null &&
                            g.getGranteePrincipal().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
            grantsGrid.setItems(filtered);
        }
    }

    // ------------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------------

    private void openCreateGrantDialog() {
        if (selectedKeyId == null) {
            showWarning(I18n.t("kms.grants.view.select.key.first"));
            return;
        }
        CreateGrantDialog dialog = new CreateGrantDialog(selectedKeyId, kmsApiService, objectMapper, this::loadGrants);
        dialog.open();
    }

    private void revokeSelectedGrant() {
        KmsDtos.ListGrantsResponse.Grant selected = grantsGrid.asSingleSelect().getValue();
        if (selected == null) {
            showWarning(I18n.t("kms.grants.view.no.grant.selected"));
            return;
        }
        RevokeGrantDialog dialog = new RevokeGrantDialog(selectedKeyId, selected, kmsApiService, this::loadGrants);
        dialog.open();
    }

    private void retireSelectedGrant() {
        KmsDtos.ListGrantsResponse.Grant selected = grantsGrid.asSingleSelect().getValue();
        if (selected == null) {
            showWarning(I18n.t("kms.grants.view.no.grant.selected"));
            return;
        }
        RetireGrantDialog dialog = new RetireGrantDialog(selectedKeyId, selected, kmsApiService, this::loadGrants);
        dialog.open();
    }

    private void showGrantDetails() {
        KmsDtos.ListGrantsResponse.Grant selected = grantsGrid.asSingleSelect().getValue();
        if (selected == null) {
            showWarning(I18n.t("kms.grants.view.no.grant.selected"));
            return;
        }
        GrantDetailsViewDialog dialog = new GrantDetailsViewDialog(selected, objectMapper);
        dialog.open();
    }

    // ------------------------------------------------------------------------
    // UI Helpers
    // ------------------------------------------------------------------------

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        grantsGrid.setVisible(!show);
        refreshButton.setEnabled(!show);
        createGrantButton.setEnabled(!show);
        revokeGrantButton.setEnabled(!show && grantsGrid.asSingleSelect().getValue() != null);
        retireGrantButton.setEnabled(!show && grantsGrid.asSingleSelect().getValue() != null);
        viewDetailsButton.setEnabled(!show && grantsGrid.asSingleSelect().getValue() != null);
        keyCombo.setEnabled(!show);
        filterField.setEnabled(!show);
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    // ------------------------------------------------------------------------
    // Helper Classes
    // ------------------------------------------------------------------------

    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }
    }
}