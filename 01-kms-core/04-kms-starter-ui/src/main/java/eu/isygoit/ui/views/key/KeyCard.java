package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.dialogs.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
class KeyCard extends VerticalLayout {

    private final KeyManagementView keyManagementView;
    private final KmsApiService kmsApiService;
    private final String keyId;
    private final KmsDtos.CreateKeyResponse.KeyMetadata metadata;
    private final String aliasOrId;
    private final String statusText;
    private final ObjectMapper objectMapper;

    public KeyCard(KeyManagementView keyManagementView,
                   KmsApiService kmsApiService,
                   ObjectMapper objectMapper,
                   String keyId,
                   KmsDtos.CreateKeyResponse.KeyMetadata metadata) {
        this.keyManagementView = keyManagementView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.metadata = metadata;
        this.aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                ? metadata.getKeyAlias() : keyId;
        this.statusText = (metadata != null && metadata.getKeyStatus() != null)
                ? metadata.getKeyStatus().name() : "UNKNOWN";
        buildCard();
    }

    public String getKeyId() {
        return keyId;
    }

    public String getAliasOrId() {
        return aliasOrId;
    }

    public String getStatusText() {
        return statusText;
    }

    private void buildCard() {
        setWidthFull();
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        getStyle().set("transition", "all 0.2s ease-in-out");
        addClassName("hover:shadow-m");

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        Span titleSpan = new Span(aliasOrId);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName(LumoUtility.TextColor.PRIMARY);

        Span statusChip = new Span(statusText);
        statusChip.addClassName(LumoUtility.FontSize.XSMALL);
        statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
        statusChip.getStyle().set("display", "inline-block");
        switch (statusText.toUpperCase()) {
            case "ENABLED":
                statusChip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
                break;
            case "DISABLED":
                statusChip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
                break;
            case "PENDING_DELETION":
                statusChip.getStyle().set("background-color", "#FFF4E5").set("color", "#B25600");
                break;
            default:
                statusChip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
        }
        HorizontalLayout titleRow = new HorizontalLayout(titleSpan, statusChip);
        titleRow.setAlignItems(Alignment.CENTER);
        titleRow.setSpacing(true);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);

        // --- Toggle status button (enable/disable) ---
        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        Button toggleStatusBtn = createIconButton(
                isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK,
                isEnabled ? "Disable key" : "Enable key"
        );
        toggleStatusBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        if (metadata != null && !"ENABLED".equalsIgnoreCase(statusText) && !"DISABLED".equalsIgnoreCase(statusText)) {
            toggleStatusBtn.setEnabled(false);
            toggleStatusBtn.setTooltipText("Key cannot be enabled/disabled in its current state");
        }
        toggleStatusBtn.addClickListener(e -> toggleKeyStatus());

        // --- Edit button ---
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias & description & tags");
        editBtn.addClickListener(e -> openUpdateDialog());

        // --- Rotation toggle button ---
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        Button rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, rotationEnabled ? "Disable rotation" : "Enable rotation");
        if (rotationEnabled) {
            rotationBtn.getStyle().set("color", "var(--lumo-success-color)"); // green
        } else {
            rotationBtn.getStyle().set("color", "var(--lumo-tertiary-text-color)"); // gray
        }
        rotationBtn.addClickListener(e -> toggleRotation());

        // --- Describe button ---
        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View details");
        describeBtn.addClickListener(e -> showKeyDetails());

        // --- Schedule deletion button ---
        Button scheduleDeleteBtn = createIconButton(VaadinIcon.CLOCK, "Schedule deletion");
        scheduleDeleteBtn.addClickListener(e -> scheduleDeletionDialog());

        // --- Cancel deletion button (visible only if pending) ---
        Button cancelDeleteBtn = createIconButton(VaadinIcon.REFRESH, "Cancel deletion");
        cancelDeleteBtn.addClickListener(e -> cancelDeletion());
        cancelDeleteBtn.setVisible("PENDING_DELETION".equalsIgnoreCase(statusText));

        // --- Permanent delete button (changes icon and behavior based on state) ---
        boolean isPendingDeletion = "PENDING_DELETION".equalsIgnoreCase(statusText);
        Button deleteBtn;
        if (isPendingDeletion) {
            deleteBtn = createIconButton(VaadinIcon.TRASH, "Permanently delete");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmPermanentDelete());
        } else {
            deleteBtn = createIconButton(VaadinIcon.BAN, "Key can only be permanently deleted when it is in PENDING_DELETION state.");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            deleteBtn.getStyle().set("opacity", "0.6");
            deleteBtn.addClickListener(e -> {
                Notification.show("Only keys in PENDING_DELETION state can be permanently deleted.", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            });
        }

        buttonBar.add(toggleStatusBtn, editBtn, rotationBtn, describeBtn, scheduleDeleteBtn, cancelDeleteBtn, deleteBtn);
        headerRow.add(titleRow, buttonBar);
        headerRow.expand(titleRow);
        add(headerRow);

        // Description
        String descText = (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isEmpty())
                ? metadata.getDescription() : "No description provided";
        Span descSpan = new Span(descText);
        descSpan.addClassName(LumoUtility.FontSize.SMALL);
        descSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        descSpan.getStyle().set("margin-top", "var(--lumo-space-xs)");
        descSpan.getStyle().set("display", "block");
        add(descSpan);

        // First meta row: spec, usage, creation date, multi-region
        HorizontalLayout metaRow1 = new HorizontalLayout();
        metaRow1.setSpacing(true);
        metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow1.getStyle().set("margin-top", "var(--lumo-space-s)");

        String keySpec = (metadata != null && metadata.getKeySpec() != null) ? metadata.getKeySpec().name() : "N/A";
        String keyUsage = (metadata != null && metadata.getKeyUsage() != null) ? metadata.getKeyUsage().name() : "N/A";
        String created = (metadata != null && metadata.getCreateDate() != null) ?
                metadata.getCreateDate().toLocalDate().toString() : "Unknown";
        String multiRegion = (metadata != null && metadata.getMultiRegion() != null && metadata.getMultiRegion())
                ? "🌍 Multi-region" : "📍 Single-region";

        metaRow1.add(new Span("Spec: " + keySpec));
        metaRow1.add(new Span("•"));
        metaRow1.add(new Span("Usage: " + keyUsage));
        metaRow1.add(new Span("•"));
        metaRow1.add(new Span("Created: " + created));
        metaRow1.add(new Span("•"));
        metaRow1.add(new Span(multiRegion));
        add(metaRow1);

        // Second meta row: key ID (if not duplicate), origin, rotation status, version
        HorizontalLayout metaRow2 = new HorizontalLayout();
        metaRow2.setSpacing(true);
        metaRow2.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow2.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow2.getStyle().set("margin-top", "var(--lumo-space-xs)");

        String keyIdDisplay = keyId;
        if (aliasOrId.equals(keyId)) {
            keyIdDisplay = null;
        }
        if (keyIdDisplay != null) {
            metaRow2.add(new Span("ID: " + keyIdDisplay));
            metaRow2.add(new Span("•"));
        }

        String origin = (metadata != null && metadata.getOrigin() != null) ? metadata.getOrigin().name() : "N/A";
        metaRow2.add(new Span("Origin: " + origin));
        metaRow2.add(new Span("•"));

        String rotation = (metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled())
                ? "✅ Rotation ON" : "❌ Rotation OFF";
        metaRow2.add(new Span(rotation));

        String version = (metadata != null && metadata.getCurrentVersion() != null && !metadata.getCurrentVersion().isEmpty())
                ? metadata.getCurrentVersion().length() > 12 ? metadata.getCurrentVersion().substring(0, 12) + "…" : metadata.getCurrentVersion()
                : "N/A";
        metaRow2.add(new Span("•"));
        metaRow2.add(new Span("Ver: " + version));
        add(metaRow2);
    }

    private Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    private void toggleKeyStatus() {
        boolean currentlyEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        new ConfirmToggleKeyStatusDialog(kmsApiService, keyId, currentlyEnabled, keyManagementView);
    }

    private void openUpdateDialog() {
        List<KmsDtos.ListResourceTagsResponse.Tag> currentTags = fetchKeyTags(keyId);
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null ? metadata.getRotationEnabled() : false;
        Integer rotationPeriod = metadata != null && metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays() : null;
        new UpdateKeyDialog(
                keyManagementView,
                kmsApiService,
                objectMapper,
                keyId,
                metadata != null ? metadata.getKeyAlias() : null,
                metadata != null ? metadata.getDescription() : null,
                currentTags,
                rotationEnabled,
                rotationPeriod
        ).open();
    }

    private void toggleRotation() {
        boolean currentlyEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        if (currentlyEnabled) {
            // Disable rotation
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Disable rotation");
            confirm.setText("Are you sure you want to disable automatic key rotation?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Disable");
            confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            confirm.addConfirmListener(event -> {
                try {
                    KmsDtos.UpdateKeyRotationRequest request = KmsDtos.UpdateKeyRotationRequest.builder()
                            .enableRotation(false)
                            .build();
                    kmsApiService.updateKeyRotation(keyId, request);
                    Notification.show("Rotation disabled", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    keyManagementView.loadKeys();
                } catch (Exception ex) {
                    Notification.show("Failed to disable rotation: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirm.open();
        } else {
            // Enable rotation: ask for rotation period
            Dialog periodDialog = new Dialog();
            periodDialog.setHeaderTitle("Enable automatic rotation");
            periodDialog.setWidth("400px");

            IntegerField periodField = new IntegerField("Rotation period (days)");
            periodField.setMin(90);
            periodField.setMax(3650);
            periodField.setValue(365);
            periodField.setStepButtonsVisible(true);
            periodField.setWidthFull();

            Button enableBtn = new Button("Enable", e -> {
                int period = periodField.getValue();
                periodDialog.close();
                try {
                    KmsDtos.UpdateKeyRotationRequest request = KmsDtos.UpdateKeyRotationRequest.builder()
                            .enableRotation(true)
                            .rotationPeriodInDays(period)
                            .build();
                    kmsApiService.updateKeyRotation(keyId, request);
                    Notification.show("Rotation enabled with period " + period + " days", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    keyManagementView.loadKeys();
                } catch (Exception ex) {
                    Notification.show("Failed to enable rotation: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            enableBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelBtn = new Button("Cancel", e -> periodDialog.close());

            periodDialog.getFooter().add(cancelBtn, enableBtn);
            periodDialog.add(periodField);
            periodDialog.open();
        }
    }

    private List<KmsDtos.ListResourceTagsResponse.Tag> fetchKeyTags(String keyId) {
        try {
            ResponseEntity<KmsDtos.ListResourceTagsResponse> response = kmsApiService.listResourceTags(keyId, 100, null);
            KmsDtos.ListResourceTagsResponse tagsResponse = response.getBody();
            if (tagsResponse != null && tagsResponse.getTags() != null) {
                return tagsResponse.getTags();
            }
        } catch (Exception e) { /* ignore */ }
        return new ArrayList<>();
    }

    private void showKeyDetails() {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null) {
                KmsDtos.CreateKeyResponse.KeyMetadata meta = desc.getKeyMetadata();
                Dialog detailsDialog = new Dialog();
                detailsDialog.setHeaderTitle("Key details");
                detailsDialog.setWidth("750px");

                VerticalLayout content = new VerticalLayout();
                content.setSpacing(true);

                content.add(detailRow("Key ID", meta.getKeyId()),
                        detailRow("WRN", meta.getWrn()),
                        detailRow("Alias", meta.getKeyAlias()),
                        detailRow("Description", meta.getDescription()),
                        detailRow("Status", meta.getKeyStatus() != null ? meta.getKeyStatus().name() : "N/A"),
                        detailRow("Key spec", meta.getKeySpec() != null ? meta.getKeySpec().name() : "N/A"),
                        detailRow("Key usage", meta.getKeyUsage() != null ? meta.getKeyUsage().name() : "N/A"),
                        detailRow("Customer master key spec", meta.getCustomerMasterKeySpec()),
                        detailRow("Origin", meta.getOrigin() != null ? meta.getOrigin().name() : "N/A"),
                        detailRow("Creation date", meta.getCreateDate() != null ? meta.getCreateDate().toString() : "N/A"),
                        detailRow("Rotation enabled", meta.getRotationEnabled() != null ? meta.getRotationEnabled().toString() : "N/A"),
                        detailRow("Current version", meta.getCurrentVersion()),
                        detailRow("Key manager", meta.getKeyManager()),
                        detailRow("Expiration model", meta.getExpirationModel() != null ? meta.getExpirationModel().name() : "N/A"),
                        detailRow("Multi-region", meta.getMultiRegion() != null ? meta.getMultiRegion().toString() : "false"));

                if (meta.getMultiRegionConfiguration() != null) {
                    try {
                        String mrConfig = objectMapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(meta.getMultiRegionConfiguration());
                        if (mrConfig != null && !mrConfig.isBlank()) {
                            content.add(detailRow("Multi-region config", mrConfig));
                        }
                    } catch (Exception e) {
                        content.add(detailRow("Multi-region config", meta.getMultiRegionConfiguration().toString()));
                    }
                }

                if (meta.getEncryptionAlgorithmSpecs() != null && !meta.getEncryptionAlgorithmSpecs().isEmpty()) {
                    content.add(detailRow("Encryption algorithms", String.join(", ", meta.getEncryptionAlgorithmSpecs())));
                }
                if (meta.getSigningAlgorithms() != null && !meta.getSigningAlgorithms().isEmpty()) {
                    content.add(detailRow("Signing algorithms", String.join(", ", meta.getSigningAlgorithms())));
                }

                List<KmsDtos.ListResourceTagsResponse.Tag> tags = fetchKeyTags(keyId);
                if (!tags.isEmpty()) {
                    Div tagsContainer = new Div();
                    tagsContainer.getStyle()
                            .set("display", "flex")
                            .set("flex-wrap", "wrap")
                            .set("gap", "var(--lumo-space-xs)")
                            .set("margin-top", "var(--lumo-space-s)");
                    Span label = new Span("Tags: ");
                    label.addClassName(LumoUtility.FontWeight.BOLD);
                    tagsContainer.add(label);
                    for (KmsDtos.ListResourceTagsResponse.Tag tag : tags) {
                        Span chip = new Span(tag.getTagKey() + "=" + tag.getTagValue());
                        chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                        chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                        chip.addClassName(LumoUtility.BorderRadius.LARGE);
                        chip.getStyle()
                                .set("background-color", "#E9ECEF")
                                .set("color", "#495057")
                                .set("white-space", "nowrap");
                        tagsContainer.add(chip);
                    }
                    content.add(tagsContainer);
                }

                try {
                    ResponseEntity<KmsDtos.GetKeyPolicyResponse> policyResponse = kmsApiService.getKeyPolicy(keyId);
                    if (policyResponse.getStatusCode().is2xxSuccessful() && policyResponse.getBody() != null) {
                        Object policyObj = policyResponse.getBody().getPolicy();
                        String prettyPolicy = null;
                        if (policyObj instanceof String) {
                            Object json = objectMapper.readValue((String) policyObj, Object.class);
                            prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                        } else if (policyObj instanceof Map) {
                            prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyObj);
                        }
                        if (prettyPolicy != null && !prettyPolicy.isBlank()) {
                            Span label = new Span("Policy: ");
                            label.addClassName(LumoUtility.FontWeight.BOLD);
                            content.add(label);
                            TextArea policyArea = new TextArea();
                            policyArea.setValue(prettyPolicy);
                            policyArea.setWidthFull();
                            policyArea.setHeight("300px");
                            policyArea.setReadOnly(true);
                            policyArea.getStyle().set("font-family", "monospace");
                            content.add(policyArea);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch or format policy for key {}", keyId, e);
                }

                detailsDialog.add(content);
                Button closeBtn = new Button("Close", e -> detailsDialog.close());
                closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                detailsDialog.getFooter().add(closeBtn);
                detailsDialog.open();
            } else {
                Notification.show("No metadata found", 3000, Notification.Position.TOP_END);
            }
        } catch (Exception e) {
            Notification.show("Failed to load details", 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private HorizontalLayout detailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.BOLD);
        labelSpan.setWidth("30%");
        Span valueSpan = new Span(value != null ? value : "-");
        valueSpan.setWidth("70%");
        row.add(labelSpan, valueSpan);
        return row;
    }

    private void scheduleDeletionDialog() {
        new ScheduleKeyDeletionDialog(kmsApiService, keyId, keyManagementView);
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(kmsApiService, keyId, aliasOrId, keyManagementView);
    }

    private void confirmPermanentDelete() {
        new PermanentKeyDeleteDialog(kmsApiService, keyId, keyManagementView);
    }
}