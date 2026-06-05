package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.BaseCard;
import eu.isygoit.ui.views.key.dialog.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
class KeyCard extends BaseCard<KeyManagementView, KmsApiService> {

    private final String keyId;
    private final ObjectMapper objectMapper;

    private DescribeKeyResponse.KeyMetadata metadata;
    private String aliasOrId;
    private String statusText;

    // Container for dynamic body components (both rows and warning)
    private final List<Component> bodyComponents = new ArrayList<>();
    private Span statusChip;
    private Span versionSpan;
    private Button rotationBtn;
    private Button versionsBtn;
    private Button moreBtn;
    private Span deletionWarningSpan;
    private ContextMenu currentContextMenu;
    // UI components
    private Span titleSpan;
    private int versionCount = 0;

    public KeyCard(KeyManagementView parentView,
                   KmsApiService kmsApiService,
                   ObjectMapper objectMapper,
                   String keyId,
                   DescribeKeyResponse.KeyMetadata metadata) {
        super(parentView, kmsApiService);
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.metadata = metadata;
        updateDerivedFields();
        initCard();
    }

    // Public accessors
    public String getKeyId() {
        return keyId;
    }

    public String getAliasOrId() {
        return aliasOrId;
    }

    public IEnumKeyStatus.Types getStatus() {
        return metadata != null ? metadata.getKeyStatus() : null;
    }

    // Refresh method
    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<DescribeKeyResponse> response = objectService.describeKey(keyId);
                if (response.getBody() != null && response.getBody().getKeyMetadata() != null) {
                    this.metadata = response.getBody().getKeyMetadata();
                    updateDerivedFields();

                    // Update header
                    titleSpan.setText(aliasOrId);
                    titleSpan.getElement().setAttribute("title", aliasOrId);
                    ChipColor color = ChipColor.fromStatus(statusText);
                    statusChip.setText(statusText);
                    statusChip.getStyle()
                            .set("background-color", color.background())
                            .set("color", color.foreground());
                    statusChip.getElement().setAttribute("title", statusText);

                    updateVersionDisplay();
                    updateRotationButton();
                    updateDeletionWarning();

                    // Rebuild body
                    rebuildBody();

                    // Refresh context menu
                    attachContextMenu(moreBtn);

                    // Reload version count
                    loadVersionCount();
                }
            } catch (Exception e) {
                log.error("Failed to refresh key card for {}", keyId, e);
            }
        }));
    }

    // BaseCard implementation
    @Override
    protected String cardCssClassName() {
        return "key-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

        titleSpan = buildTitleSpan(aliasOrId, aliasOrId);
        statusChip = buildStatusChip(statusText, statusText);

        versionSpan = new Span();
        updateVersionDisplay();
        String currentVer = metadata != null && metadata.getCurrentVersion() != null
                ? metadata.getCurrentVersion() : "N/A";
        Button copyVersionBtn = MainView.createCopyButton(VaadinIcon.COPY, currentVer, "Copy current key version");
        copyVersionBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout versionLayout = new HorizontalLayout(versionSpan, copyVersionBtn);
        versionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        versionLayout.setSpacing(false);

        left.add(titleSpan, statusChip, versionLayout);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias, description, tags, rotation");
        editBtn.addClickListener(e -> updateKey());

        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full key details");
        describeBtn.addClickListener(e -> describeKey());

        rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, "");
        rotationBtn.addClickListener(e -> toggleRotation());
        updateRotationButton();

        versionsBtn = createIconButton(VaadinIcon.CUBE, "View all key versions");
        versionsBtn.addClickListener(e -> showVersionsDialog());

        moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V, "More actions");
        attachContextMenu(moreBtn);

        return List.of(editBtn, describeBtn, rotationBtn, versionsBtn, moreBtn);
    }

    @Override
    protected void buildBodyRows() {
        // Clear previous components
        bodyComponents.clear();

        // Description row (using FILE_TEXT icon)
        HorizontalLayout descRow = createIconRow(VaadinIcon.FILE_TEXT, "Description",
                metadata != null && metadata.getDescription() != null ? metadata.getDescription() : "No description");
        bodyComponents.add(descRow);

        // Key spec & usage
        String keySpec = metadata != null && metadata.getKeySpec() != null ? metadata.getKeySpec().name() : "N/A";
        String keyUsage = metadata != null && metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : "N/A";
        bodyComponents.add(createIconRow(VaadinIcon.COG, "Key spec", keySpec));
        bodyComponents.add(createIconRow(VaadinIcon.SHIELD, "Key usage", keyUsage));

        // Created & region
        String created = metadata != null && metadata.getCreateDate() != null
                ? DateHelper.formatToHumanReadable(metadata.getCreateDate()) : "Unknown";
        String region = metadata != null && Boolean.TRUE.equals(metadata.getMultiRegion())
                ? "Multi-region" : "Single-region";
        bodyComponents.add(createIconRow(VaadinIcon.CALENDAR, "Created", created));
        bodyComponents.add(createIconRow(VaadinIcon.GLOBE, "Region", region));

        // Key ID with copy
        bodyComponents.add(createIconRowWithCopy(VaadinIcon.KEY, "Key ID", keyId, keyId));

        // Origin & rotation
        String origin = metadata != null && metadata.getOrigin() != null ? metadata.getOrigin().name() : "N/A";
        String rotation = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled())
                ? "ON (" + metadata.getRotationPeriodInDays() + " days)" : "OFF";
        bodyComponents.add(createIconRow(VaadinIcon.CLOUD, "Origin", origin));
        bodyComponents.add(createIconRow(VaadinIcon.REFRESH, "Rotation", rotation));

        // Deletion warning (Span)
        createDeletionWarningSpan();
        updateDeletionWarning();
        bodyComponents.add(deletionWarningSpan);

        // Add all components to the card
        bodyComponents.forEach(this::add);
    }

    // Helper: standard icon row
    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createIconRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createIconRow(icon, label, value);
        Button copyBtn = MainView.createCopyButton(VaadinIcon.COPY, copyValue, "Copy");
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        row.add(copyBtn);
        return row;
    }

    // Internal update helpers
    private void updateDerivedFields() {
        aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                ? metadata.getKeyAlias() : keyId;
        statusText = (metadata != null && metadata.getKeyStatus() != null)
                ? metadata.getKeyStatus().name() : "UNKNOWN";
    }

    private void updateVersionDisplay() {
        String full = (metadata != null && metadata.getCurrentVersion() != null)
                ? metadata.getCurrentVersion() : "N/A";
        String display = full.length() > 12 ? full.substring(0, 12) + "…" : full;
        versionSpan.setText(display);
        versionSpan.addClassName(LumoUtility.FontSize.XSMALL);
        versionSpan.addClassName(LumoUtility.TextColor.TERTIARY);
        versionSpan.getStyle().set("font-family", "monospace");
        versionSpan.getElement().setAttribute("title", "Current key version: " + full);
    }

    private void updateRotationButton() {
        boolean on = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        rotationBtn.setTooltipText(on ? "Disable automatic rotation" : "Enable automatic rotation");
        rotationBtn.getStyle().set("color", on
                ? "var(--lumo-success-color)"
                : "var(--lumo-tertiary-text-color)");
    }

    private void createDeletionWarningSpan() {
        deletionWarningSpan = new Span();
        deletionWarningSpan.addClassName(LumoUtility.Background.ERROR_10);
        deletionWarningSpan.addClassName(LumoUtility.TextColor.ERROR);
        deletionWarningSpan.addClassName(LumoUtility.Padding.SMALL);
        deletionWarningSpan.addClassName(LumoUtility.BorderRadius.MEDIUM);
        deletionWarningSpan.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-top", "var(--lumo-space-xs)");
        deletionWarningSpan.setVisible(false);
    }

    private void updateDeletionWarning() {
        if (deletionWarningSpan == null) return;
        boolean isPending = "PENDING_DELETION".equalsIgnoreCase(statusText);
        if (isPending) {
            String warningText = buildDeletionWarning();
            deletionWarningSpan.removeAll();
            deletionWarningSpan.add(VaadinIcon.EXCLAMATION_CIRCLE.create());
            deletionWarningSpan.add(new Span(warningText));
            deletionWarningSpan.setVisible(true);
        } else {
            deletionWarningSpan.setVisible(false);
        }
    }

    private String buildDeletionWarning() {
        if (metadata == null) return "Key is scheduled for deletion";
        LocalDateTime deletionDate = metadata.getDeletionDate();
        if (deletionDate != null) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), deletionDate);
            if (daysRemaining > 0) return "Key will be deleted in " + daysRemaining + " days";
            if (daysRemaining == 0) return "Key will be deleted today";
            return "Key deletion is overdue";
        }
        return "Key is scheduled for deletion";
    }

    private void rebuildBody() {
        // Remove all current body components
        bodyComponents.forEach(comp -> {
            if (comp.getParent().isPresent()) remove(comp);
        });
        bodyComponents.clear();
        // Rebuild
        buildBodyRows();
    }

    private void loadVersionCount() {
        try {
            ResponseEntity<ListKeyVersionsResponse> resp = objectService.listKeyVersions(keyId, 100, null);
            versionCount = resp.getBody() != null && resp.getBody().getVersions() != null
                    ? resp.getBody().getVersions().size() : 0;
        } catch (Exception e) {
            versionCount = 0;
        }
        getUI().ifPresent(ui -> ui.access(() -> {
            versionsBtn.setText("Ver (" + versionCount + ")");
            versionsBtn.setTooltipText("Total key versions: " + versionCount);
        }));
    }

    // Context menu (unchanged)
    private void attachContextMenu(Button button) {
        if (currentContextMenu != null) {
            currentContextMenu.setTarget(null);
            currentContextMenu.removeAll();
        }
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(button);
        contextMenu.setOpenOnClick(true);
        populateContextMenu(contextMenu);
        currentContextMenu = contextMenu;
    }

    private void populateContextMenu(ContextMenu contextMenu) {
        contextMenu.getItems().clear();

        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        String toggleLabel = isEnabled ? "Disable key" : "Enable key";
        VaadinIcon toggleIcon = isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK;
        HorizontalLayout toggleItem = new HorizontalLayout(toggleIcon.create(), new Span(toggleLabel));
        toggleItem.setSpacing(true);
        toggleItem.setAlignItems(FlexComponent.Alignment.CENTER);
        contextMenu.addItem(toggleItem, e -> toggleKeyStatus());

        HorizontalLayout scheduleItem = new HorizontalLayout(VaadinIcon.CLOCK.create(), new Span("Schedule deletion"));
        scheduleItem.setSpacing(true);
        scheduleItem.setAlignItems(FlexComponent.Alignment.CENTER);
        contextMenu.addItem(scheduleItem, e -> scheduleDeletion());

        if (metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled())) {
            HorizontalLayout rotateItem = new HorizontalLayout(VaadinIcon.REFRESH.create(), new Span("Rotate immediately"));
            rotateItem.setSpacing(true);
            rotateItem.setAlignItems(FlexComponent.Alignment.CENTER);
            contextMenu.addItem(rotateItem, e ->
                    new RotateKeyConfirmDialog(parentView, objectService, keyId, this::refresh).open());
        }

        boolean isPending = "PENDING_DELETION".equalsIgnoreCase(statusText);
        if (isPending) {
            HorizontalLayout cancelItem = new HorizontalLayout(VaadinIcon.REFRESH.create(), new Span("Cancel deletion"));
            cancelItem.setSpacing(true);
            cancelItem.setAlignItems(FlexComponent.Alignment.CENTER);
            contextMenu.addItem(cancelItem, e -> cancelDeletion());

            HorizontalLayout deleteItem = new HorizontalLayout(VaadinIcon.TRASH.create(), new Span("Permanently delete"));
            deleteItem.setSpacing(true);
            deleteItem.setAlignItems(FlexComponent.Alignment.CENTER);
            deleteItem.getStyle().set("color", "var(--lumo-error-color)");
            contextMenu.addItem(deleteItem, e -> confirmPermanentDelete());
        } else {
            HorizontalLayout disabledItem = new HorizontalLayout(VaadinIcon.BAN.create(), new Span("Permanently delete (not pending)"));
            disabledItem.setSpacing(true);
            disabledItem.setAlignItems(FlexComponent.Alignment.CENTER);
            disabledItem.getStyle().set("opacity", "0.5");
            contextMenu.addItem(disabledItem, e -> {
            });
        }
    }

    // Actions (all call refresh on completion)
    private void updateKey() {
        List<ListResourceTagsResponse.Tag> tags = fetchKeyTags();
        boolean rotationEnabled = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        Integer rotationPeriod = metadata != null ? metadata.getRotationPeriodInDays() : null;
        new UpdateKeyDialog(parentView, objectService, objectMapper, keyId,
                metadata != null ? metadata.getKeyAlias() : null,
                metadata != null ? metadata.getDescription() : null,
                tags, rotationEnabled, rotationPeriod, this::refresh).open();
    }

    private void describeKey() {
        new DescribeKeyDialog(parentView, objectService, objectMapper, keyId, metadata).open();
    }

    private void toggleRotation() {
        boolean currentlyEnabled = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        Integer currentPeriod = metadata != null ? metadata.getRotationPeriodInDays() : null;
        new ToggleRotationDialog(parentView, objectService, keyId, currentlyEnabled, currentPeriod, this::refresh).open();
    }

    private void showVersionsDialog() {
        new ShowKeyVersionsDialog(objectService, keyId, aliasOrId).open();
    }

    private void toggleKeyStatus() {
        new ToggleKeyStatusDialog(parentView, objectService, keyId,
                metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED, this::refresh).open();
    }

    private void scheduleDeletion() {
        new ScheduleKeyDeletionDialog(parentView, objectService, keyId,
                metadata != null ? metadata.getPendingDeletionWindowDays() : null, this::refresh).open();
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(parentView, objectService, keyId, aliasOrId, this::refresh).open();
    }

    private void confirmPermanentDelete() {
        Runnable onConfirm = () -> getUI().ifPresent(ui -> ui.access(() ->
                getParent().ifPresent(p -> {
                    if (p instanceof com.vaadin.flow.component.orderedlayout.VerticalLayout vl) vl.remove(this);
                })
        ));
        new PermanentDeleteKeyDialog(parentView, objectService, keyId, onConfirm).open();
    }

    private List<ListResourceTagsResponse.Tag> fetchKeyTags() {
        try {
            ResponseEntity<ListResourceTagsResponse> resp = objectService.listResourceTags(keyId, 100, null);
            if (resp.getBody() != null && resp.getBody().getTags() != null) return resp.getBody().getTags();
        } catch (Exception e) { /* ignore */ }
        return new ArrayList<>();
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        loadVersionCount();
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .key-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .key-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .key-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .key-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}