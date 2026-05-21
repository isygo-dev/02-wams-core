package eu.isygoit.ui.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route(value = "grants", layout = MainLayout.class)
@PageTitle("Grants")
@PermitAll
public class GrantsView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("KMS Key");
    private final Grid<KmsDtos.ListGrantsResponse.Grant> grantsGrid = new Grid<>();
    private final Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
    private final Button createGrantButton = new Button("Create Grant", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button revokeGrantButton = new Button("Revoke Grant", new Icon(VaadinIcon.BAN));
    private final Button retireGrantButton = new Button("Retire Grant", new Icon(VaadinIcon.CLOSE_CIRCLE));
    private final ProgressBar loadingBar = new ProgressBar();

    private String selectedKeyId = null;
    private List<KeyOption> keyOptions = new ArrayList<>();

    @Autowired
    public GrantsView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-grants-view");

        H2 header = new H2("Grants");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Responsive key selection toolbar
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        keyLayout.setSpacing(true);
        keyLayout.getStyle().set("flex-wrap", "wrap");
        keyLayout.addClassName("grants-key-layout");

        keyCombo.setPlaceholder("Select a KMS key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                loadGrants();
            } else {
                selectedKeyId = null;
                grantsGrid.setItems(new ArrayList<>());
            }
        });

        Button refreshKeysButton = new Button(new Icon(VaadinIcon.REFRESH));
        refreshKeysButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshKeysButton.setTooltipText("Refresh key list");
        refreshKeysButton.addClickListener(e -> loadKeyOptions());

        keyLayout.add(keyCombo, refreshKeysButton);
        add(keyLayout);

        // Responsive action bar
        HorizontalLayout actionBar = new HorizontalLayout(refreshButton, createGrantButton, revokeGrantButton, retireGrantButton);
        actionBar.setSpacing(true);
        actionBar.getStyle().set("flex-wrap", "wrap");
        actionBar.addClassName("grants-action-bar");
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        createGrantButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        revokeGrantButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        retireGrantButton.addThemeVariants(ButtonVariant.LUMO_WARNING);
        add(actionBar);

        // Grants grid – make horizontally scrollable on small screens
        grantsGrid.setWidthFull();
        grantsGrid.setHeight("500px");
        grantsGrid.getStyle().set("overflow-x", "auto");
        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getGrantId).setHeader("Grant ID").setSortable(true);
        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getGranteePrincipal).setHeader("Grantee Principal").setSortable(true);
        grantsGrid.addColumn(KmsDtos.ListGrantsResponse.Grant::getRetiringPrincipal).setHeader("Retiring Principal");
        grantsGrid.addColumn(grant -> {
            if (grant.getOperations() == null) return "[]";
            return String.join(", ", grant.getOperations());
        }).setHeader("Operations");
        grantsGrid.addColumn(grant -> {
            if (grant.getConstraints() == null) return "-";
            try {
                return objectMapper.writeValueAsString(grant.getConstraints());
            } catch (Exception e) {
                return grant.getConstraints().toString();
            }
        }).setHeader("Constraints");
        grantsGrid.addColumn(grant -> {
            if (grant.getCreateDate() == null) return "-";
            return grant.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }).setHeader("Creation Date");
        grantsGrid.addColumn(new ComponentRenderer<>(grant -> {
            Span statusChip = new Span(grant.getStatus() != null ? grant.getStatus() : "ACTIVE");
            statusChip.addClassName(LumoUtility.FontSize.XSMALL);
            statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
            statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
            if ("ACTIVE".equalsIgnoreCase(grant.getStatus())) {
                statusChip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
            } else if ("REVOKED".equalsIgnoreCase(grant.getStatus())) {
                statusChip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
            } else if ("RETIRED".equalsIgnoreCase(grant.getStatus())) {
                statusChip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
            }
            return statusChip;
        })).setHeader("Status");

        grantsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        add(grantsGrid);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Event listeners
        refreshButton.addClickListener(e -> {
            if (selectedKeyId != null) loadGrants();
        });
        createGrantButton.addClickListener(e -> openCreateGrantDialog());
        revokeGrantButton.addClickListener(e -> revokeSelectedGrant());
        retireGrantButton.addClickListener(e -> retireSelectedGrant());

        // Inject responsive CSS using JavaScript (fixes URL encoding issues)
        injectResponsiveStyles();

        // Initial load
        loadKeyOptions();
    }

    private void injectResponsiveStyles() {
        String css = """
            .grants-key-layout,
            .grants-action-bar {
                display: flex;
                flex-wrap: wrap;
                gap: var(--lumo-space-s);
                align-items: center;
            }
            @media (max-width: 768px) {
                .grants-key-layout,
                .grants-action-bar {
                    flex-direction: column;
                    align-items: stretch;
                }
                .grants-key-layout > *,
                .grants-action-bar > * {
                    width: 100% !important;
                }
                .grants-key-layout > .vaadin-combo-box {
                    width: 100% !important;
                }
                /* Make grid horizontally scrollable */
                .kms-grants-view vaadin-grid {
                    overflow-x: auto;
                }
                .kms-grants-view vaadin-grid::part(table) {
                    min-width: 900px;
                }
                /* Responsive dialog */
                .grants-create-dialog {
                    width: 90vw !important;
                    max-width: 550px !important;
                    margin: 0 auto;
                }
                .grants-create-dialog .vaadin-dialog-overlay {
                    width: 90vw !important;
                    max-width: 550px;
                }
            }
        """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // ----- Data loading helpers -----
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
                keyOptions = new ArrayList<>();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(opt -> opt.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                grantsGrid.setItems(new ArrayList<>());
            }
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
        } catch (Exception e) {
            // ignore
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
                grantsGrid.setItems(grants.getGrants());
            } else {
                grantsGrid.setItems(new ArrayList<>());
            }
        } catch (Exception e) {
            Notification.show("Failed to load grants: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            grantsGrid.setItems(new ArrayList<>());
        } finally {
            showLoading(false);
        }
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        grantsGrid.setVisible(!show);
        refreshButton.setEnabled(!show);
        createGrantButton.setEnabled(!show);
        revokeGrantButton.setEnabled(!show);
        retireGrantButton.setEnabled(!show);
        keyCombo.setEnabled(!show);
    }

    // ----- Create Grant Dialog (responsive width) -----
    private void openCreateGrantDialog() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Grant");
        dialog.addClassName("grants-create-dialog"); // Responsive CSS class

        FormLayout form = new FormLayout();
        TextField granteeField = new TextField("Grantee Principal");
        granteeField.setRequired(true);
        granteeField.setPlaceholder("e.g., arn:aws:iam::123456789012:role/ExampleRole");
        granteeField.setWidthFull();

        TextField retiringField = new TextField("Retiring Principal (optional)");
        retiringField.setPlaceholder("e.g., arn:aws:iam::123456789012:user/Admin");

        CheckboxGroup<String> operationsGroup = new CheckboxGroup<>("Operations");
        operationsGroup.setItems(
                "Decrypt", "Encrypt", "GenerateDataKey", "GenerateDataKeyWithoutPlaintext",
                "ReEncryptFrom", "ReEncryptTo", "Sign", "Verify", "GenerateMac", "VerifyMac",
                "GetPublicKey", "DescribeKey", "RetireGrant"
        );
        operationsGroup.setRequired(true);
        operationsGroup.setWidthFull();

        TextArea constraintsArea = new TextArea("Constraints (JSON)");
        constraintsArea.setPlaceholder("{\"encryptionContextSubset\": {\"key\":\"value\"}}");
        constraintsArea.setHeight("100px");

        TextField nameField = new TextField("Name (optional)");
        nameField.setPlaceholder("Friendly name");

        form.add(granteeField, retiringField, operationsGroup, constraintsArea, nameField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button createBtn = new Button("Create", e -> {
            String grantee = granteeField.getValue();
            List<String> operations = new ArrayList<>(operationsGroup.getSelectedItems());
            if (!StringUtils.hasText(grantee) || operations.isEmpty()) {
                Notification.show("Grantee principal and at least one operation are required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            KmsDtos.CreateGrantRequest.GrantConstraints constraints = null;
            if (StringUtils.hasText(constraintsArea.getValue())) {
                try {
                    Map<String, Object> constraintsMap = objectMapper.readValue(constraintsArea.getValue(), new TypeReference<>() {});
                    constraints = KmsDtos.CreateGrantRequest.GrantConstraints.builder()
                            .encryptionContextSubset((Map<String, String>) constraintsMap.get("encryptionContextSubset"))
                            .encryptionContextEquals((Map<String, String>) constraintsMap.get("encryptionContextEquals"))
                            .build();
                } catch (Exception ex) {
                    Notification.show("Invalid constraints JSON: " + ex.getMessage(), 4000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }
            dialog.close();
            try {
                KmsDtos.CreateGrantRequest request = KmsDtos.CreateGrantRequest.builder()
                        .keyId(selectedKeyId)
                        .granteePrincipal(grantee)
                        .retiringPrincipal(StringUtils.hasText(retiringField.getValue()) ? retiringField.getValue() : null)
                        .operations(operations)
                        .constraints(constraints)
                        .name(StringUtils.hasText(nameField.getValue()) ? nameField.getValue() : null)
                        .build();
                ResponseEntity<KmsDtos.CreateGrantResponse> response = kmsApiService.createGrant(selectedKeyId, request);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Grant created", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadGrants();
                } else {
                    Notification.show("Creation failed: " + response.getStatusCode(), 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.add(form);
        dialog.open();
    }

    // ----- Grant actions -----
    private void revokeSelectedGrant() {
        KmsDtos.ListGrantsResponse.Grant selected = grantsGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("No grant selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (selectedKeyId == null) return;
        try {
            kmsApiService.revokeGrant(selectedKeyId, selected.getGrantId());
            Notification.show("Grant revoked", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadGrants();
        } catch (Exception e) {
            Notification.show("Failed to revoke grant: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void retireSelectedGrant() {
        KmsDtos.ListGrantsResponse.Grant selected = grantsGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("No grant selected", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (selectedKeyId == null) return;
        try {
            KmsDtos.RetireGrantRequest request = KmsDtos.RetireGrantRequest.builder()
                    .keyId(selectedKeyId)
                    .grantId(selected.getGrantId())
                    .build();
            kmsApiService.retireGrant(request);
            Notification.show("Grant retired", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadGrants();
        } catch (Exception e) {
            Notification.show("Failed to retire grant: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Helper class for key selection
    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() { return keyId; }
        String getDisplayName() { return displayName; }
    }
}