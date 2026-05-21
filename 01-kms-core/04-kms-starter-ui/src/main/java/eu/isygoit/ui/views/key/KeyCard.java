package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
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

    // Responsive components that will be dynamically adjusted
    private HorizontalLayout headerRow;
    private HorizontalLayout metaRow1;
    private HorizontalLayout metaRow2;
    private HorizontalLayout leftPart;
    private HorizontalLayout buttonBar;

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
        addClassName("key-card");
        setWidthFull();
        setPadding(true);
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

        // --- Title and status chip ---
        Span titleSpan = new Span(aliasOrId);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName(LumoUtility.TextColor.PRIMARY);
        titleSpan.getStyle().set("word-break", "break-word");

        Span statusChip = new Span(statusText);
        statusChip.addClassName(LumoUtility.FontSize.XSMALL);
        statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
        statusChip.getStyle().set("display", "inline-block").set("white-space", "nowrap");
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

        // --- Version with copy button ---
        String versionFull = (metadata != null && metadata.getCurrentVersion() != null && !metadata.getCurrentVersion().isEmpty())
                ? metadata.getCurrentVersion() : "N/A";
        String versionDisplay = versionFull.length() > 12 ? versionFull.substring(0, 12) + "…" : versionFull;
        Span versionSpan = new Span("v" + versionDisplay);
        versionSpan.addClassName(LumoUtility.FontSize.XSMALL);
        versionSpan.addClassName(LumoUtility.TextColor.TERTIARY);
        versionSpan.getStyle().set("font-family", "monospace");

        Button copyVersionBtn = new Button(new Icon(VaadinIcon.COPY_O));
        copyVersionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        copyVersionBtn.setTooltipText("Copy key version");
        copyVersionBtn.setWidth("20px");
        copyVersionBtn.setHeight("20px");
        copyVersionBtn.addClickListener(e -> keyManagementView.copyToClipboard(versionFull));

        HorizontalLayout versionLayout = new HorizontalLayout(versionSpan, copyVersionBtn);
        versionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        versionLayout.setSpacing(false);

        // --- Left part of header (alias, status, version) ---
        leftPart = new HorizontalLayout(titleSpan, statusChip, versionLayout);
        leftPart.setAlignItems(FlexComponent.Alignment.CENTER);
        leftPart.setSpacing(true);
        leftPart.getStyle().set("flex-wrap", "wrap");

        // --- Button bar (edit, info, rotation, more) ---
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias, description & tags");
        editBtn.addClickListener(e -> updateKey());

        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View details");
        describeBtn.addClickListener(e -> describeKey());

        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        Button rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, rotationEnabled ? "Disable rotation" : "Enable rotation");
        if (rotationEnabled) {
            rotationBtn.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            rotationBtn.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        }
        rotationBtn.addClickListener(e -> toggleRotation());

        Button moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V, "More actions");
        moreBtn.addClickListener(e -> showContextMenu());

        buttonBar.add(editBtn, describeBtn, rotationBtn, moreBtn);

        // --- Assemble header row ---
        headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");
        headerRow.setSpacing(true);
        headerRow.add(leftPart, buttonBar);
        headerRow.expand(leftPart);
        add(headerRow);

        // Description
        String descText = (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isEmpty())
                ? metadata.getDescription() : "No description provided";
        Span descSpan = new Span(descText);
        descSpan.addClassName(LumoUtility.FontSize.SMALL);
        descSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        descSpan.getStyle().set("margin-top", "var(--lumo-space-xs)");
        descSpan.getStyle().set("display", "block").set("word-break", "break-word");
        add(descSpan);

        // First meta row: Spec, Usage, Creation date, Multi‑region
        metaRow1 = new HorizontalLayout();
        metaRow1.setSpacing(true);
        metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow1.getStyle().set("margin-top", "var(--lumo-space-s)");
        metaRow1.getStyle().set("flex-wrap", "wrap");

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

        // Second meta row: Key ID, Origin, Rotation status
        metaRow2 = new HorizontalLayout();
        metaRow2.setSpacing(true);
        metaRow2.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow2.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow2.getStyle().set("margin-top", "var(--lumo-space-xs)");
        metaRow2.getStyle().set("flex-wrap", "wrap");

        String keyIdDisplay = keyId;
        if (keyIdDisplay != null) {
            HorizontalLayout keyIdLayout = new HorizontalLayout();
            keyIdLayout.setSpacing(false);
            keyIdLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Span keyIdSpan = new Span("ID: " + keyIdDisplay);
            keyIdSpan.getStyle().set("margin-right", "4px");
            Button copyIdBtn = new Button(new Icon(VaadinIcon.COPY_O));
            copyIdBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            copyIdBtn.setTooltipText("Copy key ID");
            copyIdBtn.addClickListener(e -> keyManagementView.copyToClipboard(keyIdDisplay));
            copyIdBtn.setWidth("24px");
            copyIdBtn.setHeight("24px");
            keyIdLayout.add(keyIdSpan, copyIdBtn);
            metaRow2.add(keyIdLayout);
            metaRow2.add(new Span("•"));
        }

        String origin = (metadata != null && metadata.getOrigin() != null) ? metadata.getOrigin().name() : "N/A";
        metaRow2.add(new Span("Origin: " + origin));
        metaRow2.add(new Span("•"));

        String rotation = (metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled())
                ? "✅ Rotation ON" : "❌ Rotation OFF";
        metaRow2.add(new Span(rotation));

        add(metaRow2);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Add responsive resize listener
        UI.getCurrent().getPage().addBrowserWindowResizeListener(event -> {
            int width = event.getWidth();
            adjustForScreenWidth(width);
        });
        // Initial adjustment – get window width on attach
        UI.getCurrent().getPage().executeJs("return window.innerWidth")
                .then(Integer.class, width -> adjustForScreenWidth(width));
    }

    private void adjustForScreenWidth(int width) {
        boolean isMobile = width < 768;
        boolean isTablet = width >= 768 && width < 1024;

        // Header row: on small screens stack vertically
        if (isMobile) {
            headerRow.getStyle().set("flex-direction", "column");
            headerRow.setAlignItems(FlexComponent.Alignment.START);
            headerRow.setSpacing(false);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            buttonBar.getStyle().set("margin-top", "var(--lumo-space-s)");
            leftPart.getStyle().set("margin-bottom", "0");
        } else {
            headerRow.getStyle().remove("flex-direction");
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
            headerRow.setSpacing(true);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttonBar.getStyle().remove("margin-top");
        }

        // Meta rows: on mobile, reduce gap and font size
        if (isMobile) {
            metaRow1.setSpacing(false);
            metaRow2.setSpacing(false);
            metaRow1.addClassName(LumoUtility.FontSize.XXSMALL);
            metaRow2.addClassName(LumoUtility.FontSize.XXSMALL);
            metaRow1.getStyle().set("margin-bottom", "4px");
        } else {
            metaRow1.setSpacing(true);
            metaRow2.setSpacing(true);
            metaRow1.removeClassName(LumoUtility.FontSize.XXSMALL);
            metaRow2.removeClassName(LumoUtility.FontSize.XXSMALL);
            metaRow1.getStyle().remove("margin-bottom");
        }

        // For tablets, keep horizontal but ensure wrapping still works
        if (isTablet) {
            headerRow.getStyle().remove("flex-direction");
            headerRow.setSpacing(true);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            metaRow1.setSpacing(true);
            metaRow2.setSpacing(true);
        }
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

        Button scheduleDeleteBtn = new Button("Schedule deletion", new Icon(VaadinIcon.CLOCK));
        scheduleDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        scheduleDeleteBtn.setWidthFull();
        scheduleDeleteBtn.addClickListener(e -> {
            menuDialog.close();
            scheduleDeletion();
        });
        layout.add(scheduleDeleteBtn);

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