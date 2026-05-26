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
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.dto.KmsDtos.UpdateKeyRotationRequest;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainView;
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
    private final ObjectMapper objectMapper;
    private DescribeKeyResponse.KeyMetadata metadata;
    private String aliasOrId;
    private String statusText;

    // Responsive components
    private HorizontalLayout headerRow;
    private HorizontalLayout metaRow1;
    private HorizontalLayout metaRow2;
    private HorizontalLayout leftPart;
    private HorizontalLayout buttonBar;
    private Span titleSpan;
    private Span statusChip;
    private Span versionSpan;
    private Span descSpan;
    private Button editBtn;
    private Button describeBtn;
    private Button rotationBtn;
    private Button versionsBtn;
    private Button moreBtn;

    // Version button
    private int versionCount = 0;

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
        updateDerivedFields();
        buildCard();
        addClassName("key-card");
        setWidthFull();
        setPadding(true);
    }

    private void updateDerivedFields() {
        this.aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                ? metadata.getKeyAlias() : keyId;
        this.statusText = (metadata != null && metadata.getKeyStatus() != null)
                ? metadata.getKeyStatus().name() : "UNKNOWN";
    }

    public String getKeyId() {
        return keyId;
    }

    public String getAliasOrId() {
        return aliasOrId;
    }

    public IEnumKeyStatus.Types getStatus() {
        return this.metadata != null ? this.metadata.getKeyStatus() : null;
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

        // --- Title and status chip ---
        titleSpan = new Span(aliasOrId);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName(LumoUtility.TextColor.PRIMARY);
        titleSpan.getStyle().set("word-break", "break-word");
        titleSpan.getElement().setAttribute("title", aliasOrId);

        statusChip = new Span(statusText);
        statusChip.addClassName(LumoUtility.FontSize.XSMALL);
        statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
        statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
        statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
        statusChip.getStyle().set("display", "inline-block").set("white-space", "nowrap");
        statusChip.getElement().setAttribute("title", "Key status: " + statusText);
        updateStatusChipStyle();

        // --- Version with copy button (using MainView.createCopyButton) ---
        versionSpan = new Span();
        updateVersionDisplay();

        // Copy button for version
        Button copyVersionBtn = MainView.createCopyButton(VaadinIcon.COPY_O,
                (metadata != null && metadata.getCurrentVersion() != null) ? metadata.getCurrentVersion() : "N/A",
                "Copy current key version");

        // --- Left part of header (alias, status, version) ---
        HorizontalLayout versionLayout = new HorizontalLayout(versionSpan, copyVersionBtn);
        versionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        versionLayout.setSpacing(false);
        leftPart = new HorizontalLayout(titleSpan, statusChip, versionLayout);
        leftPart.setAlignItems(FlexComponent.Alignment.CENTER);
        leftPart.setSpacing(true);
        leftPart.getStyle().set("flex-wrap", "wrap");

        // --- Button bar (edit, info, rotation, more, versions) ---
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias, description, tags, and rotation settings");
        editBtn.addClickListener(e -> updateKey());

        describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full key details");
        describeBtn.addClickListener(e -> describeKey());

        rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, "");
        rotationBtn.addClickListener(e -> toggleRotation());
        updateRotationButton();

        moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V, "More actions (enable/disable, schedule deletion, rotate immediately, etc.)");
        moreBtn.addClickListener(e -> showContextMenu());

        versionsBtn = createIconButton(VaadinIcon.CUBE, "View all key versions");
        versionsBtn.setText("Ver (...)"); // will be updated later
        versionsBtn.addClickListener(e -> showVersionsDialog());

        buttonBar.add(editBtn, describeBtn, rotationBtn, versionsBtn, moreBtn);

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
        descSpan = new Span();
        updateDescription();
        add(descSpan);

        // First meta row: Spec, Usage, Creation date, Multi‑region
        metaRow1 = new HorizontalLayout();
        metaRow1.setSpacing(true);
        metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow1.getStyle().set("margin-top", "var(--lumo-space-s)");
        metaRow1.getStyle().set("flex-wrap", "wrap");
        updateMetaRow1();
        add(metaRow1);

        // Second meta row: Key ID, Origin, Rotation status
        metaRow2 = new HorizontalLayout();
        metaRow2.setSpacing(true);
        metaRow2.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow2.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow2.getStyle().set("margin-top", "var(--lumo-space-xs)");
        metaRow2.getStyle().set("flex-wrap", "wrap");
        updateMetaRow2();
        add(metaRow2);
    }

    private void updateStatusChipStyle() {
        statusChip.setText(statusText);
        statusChip.getStyle().clear();
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
    }

    private void updateVersionDisplay() {
        String versionFull = (metadata != null && metadata.getCurrentVersion() != null && !metadata.getCurrentVersion().isEmpty())
                ? metadata.getCurrentVersion() : "N/A";
        String versionDisplay = versionFull.length() > 12 ? versionFull.substring(0, 12) + "…" : versionFull;
        versionSpan.setText("v" + versionDisplay);
        versionSpan.addClassName(LumoUtility.FontSize.XSMALL);
        versionSpan.addClassName(LumoUtility.TextColor.TERTIARY);
        versionSpan.getStyle().set("font-family", "monospace");
        versionSpan.getElement().setAttribute("title", "Current key version: " + versionFull);
    }

    private void updateDescription() {
        String descText = (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isEmpty())
                ? metadata.getDescription() : "No description provided";
        descSpan.setText(descText);
        descSpan.addClassName(LumoUtility.FontSize.SMALL);
        descSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        descSpan.getStyle().set("margin-top", "var(--lumo-space-xs)");
        descSpan.getStyle().set("display", "block").set("word-break", "break-word");
        descSpan.getElement().setAttribute("title", descText);
    }

    private void updateMetaRow1() {
        metaRow1.removeAll();
        String keySpec = (metadata != null && metadata.getKeySpec() != null) ? metadata.getKeySpec().name() : "N/A";
        String keyUsage = (metadata != null && metadata.getKeyUsage() != null) ? metadata.getKeyUsage().name() : "N/A";
        String created = (metadata != null && metadata.getCreateDate() != null) ?
                metadata.getCreateDate().toLocalDate().toString() : "Unknown";
        String multiRegion = (metadata != null && metadata.getMultiRegion() != null && metadata.getMultiRegion())
                ? "🌍 Multi-region" : "📍 Single-region";

        Span specSpan = new Span("Spec: " + keySpec);
        Span usageSpan = new Span("Usage: " + keyUsage);
        Span createdSpan = new Span("Created: " + created);
        Span regionSpan = new Span(multiRegion);

        specSpan.getElement().setAttribute("title", keySpec);
        usageSpan.getElement().setAttribute("title", keyUsage);
        createdSpan.getElement().setAttribute("title", created);
        regionSpan.getElement().setAttribute("title", multiRegion);

        metaRow1.add(specSpan, new Span("•"), usageSpan, new Span("•"), createdSpan, new Span("•"), regionSpan);
    }

    private void updateMetaRow2() {
        metaRow2.removeAll();
        if (keyId != null) {
            HorizontalLayout keyIdLayout = new HorizontalLayout();
            keyIdLayout.setSpacing(false);
            keyIdLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            Span keyIdSpan = new Span("ID: " + keyId);
            keyIdSpan.getStyle().set("margin-right", "4px");
            keyIdSpan.getElement().setAttribute("title", keyId);
            // Use MainView.createCopyButton for key ID
            Button copyIdBtn = MainView.createCopyButton(VaadinIcon.COPY_O, keyId, "Copy full key ID");
            keyIdLayout.add(keyIdSpan, copyIdBtn);
            metaRow2.add(keyIdLayout);
            metaRow2.add(new Span("•"));
        }
        String origin = (metadata != null && metadata.getOrigin() != null) ? metadata.getOrigin().name() : "N/A";
        String rotation = (metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled())
                ? "✅ Rotation ON (" + metadata.getRotationPeriodInDays() + " Days)" : "❌ Rotation OFF";
        Span originSpan = new Span("Origin: " + origin);
        Span rotationSpan = new Span(rotation);
        originSpan.getElement().setAttribute("title", origin);
        rotationSpan.getElement().setAttribute("title", rotation);
        metaRow2.add(originSpan);
        metaRow2.add(new Span("•"));
        metaRow2.add(rotationSpan);
    }

    private void updateRotationButton() {
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        rotationBtn.setTooltipText(rotationEnabled ? "Disable automatic rotation" : "Enable automatic rotation");
        if (rotationEnabled) {
            rotationBtn.getStyle().set("color", "var(--lumo-success-color)");
        } else {
            rotationBtn.getStyle().set("color", "var(--lumo-tertiary-text-color)");
        }
    }

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
                if (response.getBody() != null && response.getBody().getKeyMetadata() != null) {
                    this.metadata = response.getBody().getKeyMetadata();
                    updateDerivedFields();
                    updateStatusChipStyle();
                    titleSpan.setText(aliasOrId);
                    titleSpan.getElement().setAttribute("title", aliasOrId);
                    updateVersionDisplay();
                    updateDescription();
                    updateMetaRow1();
                    updateMetaRow2();
                    updateRotationButton();
                    loadVersionCount();
                }
            } catch (Exception e) {
                log.error("Failed to refresh key card for {}", keyId, e);
            }
        }));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        UI.getCurrent().getPage().addBrowserWindowResizeListener(event -> {
            int width = event.getWidth();
            adjustForScreenWidth(width);
        });
        UI.getCurrent().getPage().executeJs("return window.innerWidth")
                .then(Integer.class, width -> adjustForScreenWidth(width));
        loadVersionCount();
    }

    private void adjustForScreenWidth(int width) {
        boolean isMobile = width < 768;
        boolean isTablet = width >= 768 && width < 1024;

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

        if (isTablet) {
            headerRow.getStyle().remove("flex-direction");
            headerRow.setSpacing(true);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            metaRow1.setSpacing(true);
            metaRow2.setSpacing(true);
        }
    }

    private Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    private void loadVersionCount() {
        try {
            ResponseEntity<ListKeyVersionsResponse> response =
                    kmsApiService.listKeyVersions(keyId, 100, null);
            if (response.getBody() != null && response.getBody().getVersions() != null) {
                versionCount = response.getBody().getVersions().size();
            } else {
                versionCount = 0;
            }
        } catch (Exception e) {
            versionCount = 0;
        }
        getUI().ifPresent(ui -> ui.access(() -> {
            versionsBtn.setText("Ver (" + versionCount + ")");
            versionsBtn.setTooltipText("Total key versions: " + versionCount);
        }));
    }

    private void showVersionsDialog() {
        new KeyVersionsDialog(kmsApiService, keyId, aliasOrId).open();
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
                    Notification.show("Rotation disabled", 6000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refresh();
                } catch (Exception ex) {
                    Notification.show("Failed to disable rotation: " + ex.getMessage(), 6000, Notification.Position.TOP_END)
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
            periodField.setTooltipText("Number of days between automatic key rotations (90–3650)");

            Button enableBtn = new Button("Enable", e -> {
                int period = periodField.getValue();
                periodDialog.close();
                try {
                    UpdateKeyRotationRequest request = UpdateKeyRotationRequest.builder()
                            .enableRotation(true)
                            .rotationPeriodInDays(period)
                            .build();
                    kmsApiService.updateKeyRotation(keyId, request);
                    Notification.show("Rotation enabled with period " + period + " days", 6000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refresh();
                } catch (Exception ex) {
                    Notification.show("Failed to enable rotation: " + ex.getMessage(), 6000, Notification.Position.TOP_END)
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
        } else {
            toggleStatusBtn.setTooltipText(isEnabled ? "Disable this key" : "Enable this key");
        }
        toggleStatusBtn.addClickListener(e -> {
            menuDialog.close();
            toggleKeyStatus();
        });
        layout.add(toggleStatusBtn);

        Button scheduleDeleteBtn = new Button("Schedule deletion", new Icon(VaadinIcon.CLOCK));
        scheduleDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        scheduleDeleteBtn.setWidthFull();
        scheduleDeleteBtn.setTooltipText("Schedule key deletion after a waiting period");
        scheduleDeleteBtn.addClickListener(e -> {
            menuDialog.close();
            scheduleDeletion();
        });
        layout.add(scheduleDeleteBtn);

        // Rotate immediately button - only shown if automatic rotation is enabled
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null && metadata.getRotationEnabled();
        if (rotationEnabled) {
            Button rotateNowBtn = new Button("Rotate immediately", new Icon(VaadinIcon.REFRESH));
            rotateNowBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            rotateNowBtn.setWidthFull();
            rotateNowBtn.setTooltipText("Create a new key version immediately (manual rotation)");
            rotateNowBtn.addClickListener(e -> {
                menuDialog.close();
                new RotateKeyConfirmDialog(keyManagementView, kmsApiService, keyId, this::refresh).open();
            });
            layout.add(rotateNowBtn);
        }

        if ("PENDING_DELETION".equalsIgnoreCase(statusText)) {
            Button cancelDeleteBtn = new Button("Cancel deletion", new Icon(VaadinIcon.REFRESH));
            cancelDeleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            cancelDeleteBtn.setWidthFull();
            cancelDeleteBtn.setTooltipText("Cancel pending deletion and restore the key");
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
            deleteBtn.setTooltipText("Immediately delete the key (cannot be undone)");
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

    private List<ListResourceTagsResponse.Tag> fetchKeyTags() {
        try {
            ResponseEntity<ListResourceTagsResponse> response = kmsApiService.listResourceTags(keyId, 100, null);
            ListResourceTagsResponse tagsResponse = response.getBody();
            if (tagsResponse != null && tagsResponse.getTags() != null) {
                return tagsResponse.getTags();
            }
        } catch (Exception e) {
            // ignore
        }
        return new ArrayList<>();
    }

    private void toggleKeyStatus() {
        boolean currentlyEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        new ToggleKeyStatusDialog(keyManagementView, kmsApiService, this::refresh, keyId, currentlyEnabled).open();
    }

    private void updateKey() {
        List<ListResourceTagsResponse.Tag> currentTags = fetchKeyTags();
        boolean rotationEnabled = metadata != null && metadata.getRotationEnabled() != null ? metadata.getRotationEnabled() : false;
        Integer rotationPeriod = metadata != null && metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays() : null;
        new UpdateKeyDialog(
                keyManagementView,
                kmsApiService,
                this::refresh,
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
        new ScheduleKeyDeletionDialog(keyManagementView, kmsApiService, this::refresh, keyId).open();
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(keyManagementView, kmsApiService, this::refresh, keyId, aliasOrId).open();
    }

    private void confirmPermanentDelete() {
        new PermanentKeyDeleteDialog(keyManagementView, kmsApiService, () -> {
            getUI().ifPresent(ui -> ui.access(() -> {
                getParent().ifPresent(parent -> {
                    if (parent instanceof VerticalLayout) {
                        ((VerticalLayout) parent).remove(this);
                    }
                });
            }));
        }, keyId).open();
    }

    private void describeKey() {
        new DescribeKeyDialog(keyManagementView, kmsApiService, this::refresh, objectMapper, keyId, metadata).open();
    }
}