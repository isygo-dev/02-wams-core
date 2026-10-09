package eu.isygoit.ui.kms.views.cryptography.keyGrants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
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
    private final RowCardList<KmsDtos.ListGrantsResponse.Grant> grantsList = new RowCardList<>();
    private final Button refreshButton = new Button(I18n.t("kms.grants.view.refresh.button"), new Icon(VaadinIcon.REFRESH));
    private final Button createGrantButton = new Button(I18n.t("kms.grants.view.create.grant.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
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
                grantsList.setItems(new ArrayList<>());
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
        HorizontalLayout actionBar = new HorizontalLayout(refreshButton, createGrantButton);
        actionBar.setSpacing(true);
        actionBar.addClassName("grants-action-bar");

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClassName("wams-action-btn");
        refreshButton.setTooltipText(I18n.t("kms.grants.view.refresh.grants.tooltip"));
        createGrantButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        createGrantButton.setTooltipText(I18n.t("kms.grants.view.create.grant.tooltip"));

        add(actionBar);
    }

    private void buildGrantsGrid() {
        grantsList.setWidthFull();
        grantsList.cardFactory(this::buildGrantCard);

        add(grantsList);
    }

    private RowCard buildGrantCard(KmsDtos.ListGrantsResponse.Grant grant) {
        String status = grant.getStatus() != null ? grant.getStatus() : "ACTIVE";
        return RowCard.create()
                .title(grant.getGrantId())
                .tag(KmsEnumTag.ofValues(grant.getOperations(), "kms.enum"))
                .tag(KmsEnumTag.ofValue(status, null))
                .fact(I18n.t("kms.grants.view.grid.column.grantee"), grant.getGranteePrincipal())
                .fact(I18n.t("kms.grants.view.grid.column.retiring"), grant.getRetiringPrincipal())
                .action(VaadinIcon.EYE, I18n.t("kms.grants.view.details.button"), () -> showGrantDetails(grant))
                .action(VaadinIcon.CLOSE_CIRCLE, I18n.t("kms.grants.view.retire.button"), () -> retireGrant(grant))
                .dangerAction(VaadinIcon.BAN, I18n.t("kms.grants.view.revoke.button"), () -> revokeGrant(grant));
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
                grantsList.setItems(new ArrayList<>());
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
                grantsList.setItems(new ArrayList<>());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.grants.view.load.grants.error", errorMsg));
            log.error("Failed to load grants for key {}: {}", selectedKeyId, errorMsg);
            allGrants.clear();
            grantsList.setItems(new ArrayList<>());
        } catch (Exception e) {
            showError(I18n.t("kms.grants.view.load.grants.error", e.getMessage()));
            log.error("Failed to load grants for key {}: {}", selectedKeyId, e.getMessage());
            allGrants.clear();
            grantsList.setItems(new ArrayList<>());
        } finally {
            showLoading(false);
        }
    }

    private void applyFilter() {
        String filter = filterField.getValue();
        clearFilterButton.setEnabled(StringUtils.hasText(filter));
        if (!StringUtils.hasText(filter)) {
            grantsList.setItems(allGrants);
        } else {
            List<KmsDtos.ListGrantsResponse.Grant> filtered = allGrants.stream()
                    .filter(g -> g.getGranteePrincipal() != null &&
                            g.getGranteePrincipal().toLowerCase().contains(filter.toLowerCase()))
                    .collect(Collectors.toList());
            grantsList.setItems(filtered);
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

    private void revokeGrant(KmsDtos.ListGrantsResponse.Grant grant) {
        RevokeGrantDialog dialog = new RevokeGrantDialog(selectedKeyId, grant, kmsApiService, this::loadGrants);
        dialog.open();
    }

    private void retireGrant(KmsDtos.ListGrantsResponse.Grant grant) {
        RetireGrantDialog dialog = new RetireGrantDialog(selectedKeyId, grant, kmsApiService, this::loadGrants);
        dialog.open();
    }

    private void showGrantDetails(KmsDtos.ListGrantsResponse.Grant grant) {
        GrantDetailsViewDialog dialog = new GrantDetailsViewDialog(grant, objectMapper);
        dialog.open();
    }

    // ------------------------------------------------------------------------
    // UI Helpers
    // ------------------------------------------------------------------------

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        grantsList.setVisible(!show);
        refreshButton.setEnabled(!show);
        createGrantButton.setEnabled(!show);
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