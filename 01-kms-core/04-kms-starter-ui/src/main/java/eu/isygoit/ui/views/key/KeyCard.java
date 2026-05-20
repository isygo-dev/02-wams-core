package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.dto.KmsDtos.UpdateKeyRotationRequest;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.key.dialog.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class KeyCard extends VerticalLayout {

    private final KeyManagementView keyManagementView;
    private final KmsApiService kmsApiService;
    private final String keyId;
    private final DescribeKeyResponse.KeyMetadata metadata;
    private final String aliasOrId;
    private final String statusText;
    private final ObjectMapper objectMapper;

    public KeyCard(KeyManagementView keyManagementView,
                   KmsApiService kmsApiService,
                   ObjectMapper objectMapper,
                   String keyId,
                   DescribeKeyResponse.KeyMetadata metadata) {
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

    public String getKeyId() { return keyId; }
    public String getAliasOrId() { return aliasOrId; }
    public String getStatusText() { return statusText; }

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
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

        // Title and status chip
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
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);

        // ---- Compact button bar (Edit, Info, Rotation, More) ----
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);

        // Edit
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias, description & tags");
        editBtn.addClickListener(e -> updateKey());

        // Info
        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View details");
        describeBtn.addClickListener(e -> describeKey());

        // Rotation toggle
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        Button rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, rotationEnabled ? "Disable rotation" : "Enable rotation");
        if (rotationEnabled) {
            rotationBtn.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            rotationBtn.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        }
        rotationBtn.addClickListener(e -> toggleRotation());

        // More actions (three dots)
        Button moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V, "More actions");
        moreBtn.addClickListener(e -> showContextMenu());

        buttonBar.add(editBtn, describeBtn, rotationBtn, moreBtn);
        // ========================================================

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

        // First meta row: Spec, Usage, Creation date, Multi‑region
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

        // Second meta row: Key ID (if different from alias), Origin, Rotation status, Version
        HorizontalLayout metaRow2 = new HorizontalLayout();
        metaRow2.setSpacing(true);
        metaRow2.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow2.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow2.getStyle().set("margin-top", "var(--lumo-space-xs)");

        String keyIdDisplay = keyId;
        /*if (aliasOrId.equals(keyId)) {
            keyIdDisplay = null;
        }*/
        if (keyIdDisplay != null) {
            // Instead of a plain Span, create a layout with copy button
            HorizontalLayout keyIdLayout = new HorizontalLayout();
            keyIdLayout.setSpacing(false);
            keyIdLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Span keyIdSpan = new Span("ID: " + keyIdDisplay);
            keyIdSpan.getStyle().set("margin-right", "4px");
            Button copyBtn = new Button(new Icon(VaadinIcon.COPY_O));
            copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            copyBtn.setTooltipText("Copy key ID");
            copyBtn.addClickListener(e -> keyManagementView.copyToClipboard(keyIdDisplay));
            copyBtn.setWidth("24px");
            copyBtn.setHeight("24px");
            keyIdLayout.add(keyIdSpan, copyBtn);
            metaRow2.add(keyIdLayout);
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

    // ========== Helper methods ==========

    private Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    private void toggleRotation() {
        boolean currentlyEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        if (currentlyEnabled) {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Disable rotation");
            confirm.setText("Are you sure you want to disable automatic key rotation?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Disable");
            confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            confirm.addConfirmListener(event -> {
                try {
                    UpdateKeyRotationRequest request = UpdateKeyRotationRequest.builder()
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
                    UpdateKeyRotationRequest request = UpdateKeyRotationRequest.builder()
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

    private void showContextMenu() {
        Dialog menuDialog = new Dialog();
        menuDialog.setHeaderTitle("Key actions");
        menuDialog.setWidth("280px");
        menuDialog.setCloseOnOutsideClick(true);
        menuDialog.setCloseOnEsc(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        // Enable/Disable button
        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        Button toggleStatusBtn = new Button(isEnabled ? "Disable key" : "Enable key", new Icon(isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK));
        toggleStatusBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toggleStatusBtn.setWidthFull();
        if (metadata != null && !"ENABLED".equalsIgnoreCase(statusText) && !"DISABLED".equalsIgnoreCase(statusText)) {
            toggleStatusBtn.setEnabled(false);
            toggleStatusBtn.setTooltipText("Key cannot be enabled/disabled in its current state");
        }
        toggleStatusBtn.addClickListener(e -> {
            menuDialog.close();
            toggleKeyStatus();
        });
        layout.add(toggleStatusBtn);

        // Schedule deletion
        Button scheduleDeleteBtn = new Button("Schedule deletion", new Icon(VaadinIcon.CLOCK));
        scheduleDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        scheduleDeleteBtn.setWidthFull();
        scheduleDeleteBtn.addClickListener(e -> {
            menuDialog.close();
            scheduleDeletion();
        });
        layout.add(scheduleDeleteBtn);

        // Cancel deletion (only if pending)
        if ("PENDING_DELETION".equalsIgnoreCase(statusText)) {
            Button cancelDeleteBtn = new Button("Cancel deletion", new Icon(VaadinIcon.REFRESH));
            cancelDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            cancelDeleteBtn.setWidthFull();
            cancelDeleteBtn.addClickListener(e -> {
                menuDialog.close();
                cancelDeletion();
            });
            layout.add(cancelDeleteBtn);
        }

        // Permanent delete
        boolean isPendingDeletion = "PENDING_DELETION".equalsIgnoreCase(statusText);
        if (isPendingDeletion) {
            Button deleteBtn = new Button("Permanently delete", new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.setWidthFull();
            deleteBtn.addClickListener(e -> {
                menuDialog.close();
                confirmPermanentDelete();
            });
            layout.add(deleteBtn);
        } else {
            Button disabledDeleteBtn = new Button("Permanently delete (not pending)", new Icon(VaadinIcon.BAN));
            disabledDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            disabledDeleteBtn.setEnabled(false);
            disabledDeleteBtn.setTooltipText("Key can only be permanently deleted when it is in PENDING_DELETION state.");
            disabledDeleteBtn.setWidthFull();
            layout.add(disabledDeleteBtn);
        }

        menuDialog.add(layout);
        menuDialog.open();
    }

    private List<ListResourceTagsResponse.Tag> fetchKeyTags(String keyId) {
        try {
            ResponseEntity<ListResourceTagsResponse> response = kmsApiService.listResourceTags(keyId, 100, null);
            ListResourceTagsResponse tagsResponse = response.getBody();
            if (tagsResponse != null && tagsResponse.getTags() != null) {
                return tagsResponse.getTags();
            }
        } catch (Exception e) { /* ignore */ }
        return new ArrayList<>();
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

    private void toggleKeyStatus() {
        boolean currentlyEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        new ToggleKeyStatusDialog(keyManagementView, kmsApiService, keyManagementView::loadAliasesAndKeys, keyId, currentlyEnabled).open();
    }

    private void updateKey() {
        List<ListResourceTagsResponse.Tag> currentTags = fetchKeyTags(keyId);
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null ? metadata.getRotationEnabled() : false;
        Integer rotationPeriod = metadata != null && metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays() : null;
        new UpdateKeyDialog(
                keyManagementView,
                kmsApiService,
                keyManagementView::loadAliasesAndKeys,
                objectMapper,
                keyId,
                metadata != null ? metadata.getKeyAlias() : null,
                metadata != null ? metadata.getDescription() : null,
                currentTags,
                rotationEnabled,
                rotationPeriod
        ).open();
    }

    private void scheduleDeletion() {
        new ScheduleKeyDeletionDialog(keyManagementView, kmsApiService, keyManagementView::loadAliasesAndKeys, keyId).open();
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(keyManagementView, kmsApiService, keyManagementView::loadAliasesAndKeys, keyId, aliasOrId).open();
    }

    private void confirmPermanentDelete() {
        new PermanentKeyDeleteDialog(keyManagementView, kmsApiService, keyManagementView::loadAliasesAndKeys, keyId).open();
    }

    private void describeKey() {
        new DescribeKeyDialog(keyManagementView, kmsApiService, keyManagementView::loadAliasesAndKeys, objectMapper, keyId, metadata).open();
    }
}