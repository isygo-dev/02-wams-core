package eu.isygoit.ui.kms.views.cryptography.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.key.dialog.*;
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

    // Dedicated body container – cleared and rebuilt on refresh
    private final VerticalLayout bodyContainer = new VerticalLayout();

    private DescribeKeyResponse.KeyMetadata metadata;
    private String aliasOrId;
    private String statusText;

    // UI components (updated on refresh)
    private Span titleSpan;
    private Span statusChip;
    private Span versionSpan;
    private Button rotationBtn;
    private Button versionsBtn;
    private Button moreBtn;
    private Span deletionWarningSpan;
    private ContextMenu currentContextMenu;
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

        // Setup body container
        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.setFlexGrow(1);
        bodyContainer.addClassName("key-card-body");
        add(bodyContainer);

        initCard();
    }

    // ─── Public accessors ─────────────────────────────────────────────────────

    public String getKeyId() {
        return keyId;
    }

    public String getAliasOrId() {
        return aliasOrId;
    }

    public IEnumKeyStatus.Types getStatus() {
        return metadata != null ? metadata.getKeyStatus() : null;
    }

    // ─── Refresh – fully reloads the card and context menu ──────────────────

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
                    bodyContainer.removeAll();
                    buildBodyRows();

                    // Rebuild context menu and force re‑attach
                    attachContextMenu(moreBtn);
                    moreBtn.setVisible(false);
                    moreBtn.setVisible(true);

                    loadVersionCount();
                }
            } catch (Exception e) {
                log.error("Failed to refresh key card for {}", keyId, e);
            }
        }));
    }

    // ─── BaseCard implementation ─────────────────────────────────────────────

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
        Button copyVersionBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, currentVer, "Copy current key version");
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
        bodyContainer.removeAll();

        // Description (with icon)
        bodyContainer.add(createIconRow(VaadinIcon.FILE_TEXT, "Description",
                metadata != null && metadata.getDescription() != null ? metadata.getDescription() : "No description"));

        // Key spec & usage
        bodyContainer.add(createIconRow(VaadinIcon.COG, "Key spec",
                metadata != null && metadata.getKeySpec() != null ? metadata.getKeySpec().name() : "N/A"));
        bodyContainer.add(createIconRow(VaadinIcon.SHIELD, "Key usage",
                metadata != null && metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : "N/A"));

        // Created & region
        String created = metadata != null && metadata.getCreateDate() != null
                ? DateHelper.formatToHumanReadable(metadata.getCreateDate()) : "Unknown";
        bodyContainer.add(createIconRow(VaadinIcon.CALENDAR, "Created", created));
        bodyContainer.add(createIconRow(VaadinIcon.GLOBE, "Region",
                metadata != null && Boolean.TRUE.equals(metadata.getMultiRegion()) ? "Multi-region" : "Single-region"));

        // Key ID with copy
        bodyContainer.add(createIconRowWithCopy(VaadinIcon.KEY, "Key ID", keyId, keyId));

        // Origin with color coding
        String originLabel = metadata != null && metadata.getOrigin() != null ? metadata.getOrigin().name() : "N/A";
        String originColor = getOriginColor(originLabel);
        bodyContainer.add(createIconRowWithColor(VaadinIcon.CLOUD, "Origin", originLabel, originColor));

        // Rotation with color coding
        String rotationLabel = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled())
                ? "ON (" + metadata.getRotationPeriodInDays() + " days)" : "OFF";
        String rotationColor = Boolean.TRUE.equals(metadata.getRotationEnabled()) ? "var(--lumo-success-color)" : "var(--lumo-tertiary-text-color)";
        bodyContainer.add(createIconRowWithColor(VaadinIcon.REFRESH, "Rotation", rotationLabel, rotationColor));

        // Deletion warning
        createDeletionWarningSpan();
        updateDeletionWarning();
        bodyContainer.add(deletionWarningSpan);
    }

    // ─── Helper row builders with color support ─────────────────────────────

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        return createIconRowWithColor(icon, label, value, "var(--lumo-primary-text-color)");
    }

    private HorizontalLayout createIconRowWithColor(VaadinIcon icon, String label, String value, String color) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");
        valueSpan.getStyle().set("color", color);

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createIconRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createIconRow(icon, label, value);
        Button copyBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, copyValue, "Copy");
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        row.add(copyBtn);
        return row;
    }

    private String getOriginColor(String origin) {
        if (origin == null) return "var(--lumo-tertiary-text-color)";
        if (origin.equalsIgnoreCase(IEnumKeyOrigin.Types.WAMS_KMS.name())) {
            return "var(--lumo-primary-color)";
        }
        if (origin.equalsIgnoreCase(IEnumKeyOrigin.Types.EXTERNAL.name())) {
            return "var(--lumo-warning-color)";
        }
        return "var(--lumo-tertiary-text-color)";
    }

    // ─── Internal update helpers ─────────────────────────────────────────────

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
        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        boolean isPending = "PENDING_DELETION".equalsIgnoreCase(statusText);
        boolean isActive = isEnabled && !isPending;

        boolean rotationOn = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());

        if (rotationOn) {
            rotationBtn.setTooltipText(isActive ? "Disable automatic rotation" : "Rotation is enabled but key is not active");
            rotationBtn.getStyle().set("color", isActive ? "var(--lumo-success-color)" : "var(--lumo-tertiary-text-color)");
        } else {
            rotationBtn.setTooltipText(isActive ? "Enable automatic rotation" : "Key must be enabled to configure rotation");
            rotationBtn.getStyle().set("color", isActive ? "var(--lumo-primary-color)" : "var(--lumo-tertiary-text-color)");
        }

        rotationBtn.setEnabled(isActive);
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
            Icon warningIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            warningIcon.setColor("var(--lumo-error-color)");
            deletionWarningSpan.add(warningIcon);
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

    // ─── Context menu ────────────────────────────────────────────────────────

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
        boolean isPending = "PENDING_DELETION".equalsIgnoreCase(statusText);
        boolean hasRotation = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());

        // ── 1. Toggle status ──────────────────────────────────────────────────
        String toggleLabel = isEnabled ? "Disable key" : "Enable key";
        VaadinIcon toggleIcon = isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK;
        MenuItem toggleItem = createMenuItem(contextMenu, toggleIcon, toggleLabel);
        toggleItem.setEnabled(!isPending);
        toggleItem.addClickListener(e -> toggleKeyStatus());

        // ── 2. Schedule deletion ──────────────────────────────────────────────
        MenuItem scheduleItem = createMenuItem(contextMenu, VaadinIcon.CLOCK, "Schedule deletion");
        scheduleItem.setEnabled(!isPending);
        scheduleItem.addClickListener(e -> scheduleDeletion());

        // ── 3. Rotate immediately ─────────────────────────────────────────────
        MenuItem rotateItem = createMenuItem(contextMenu, VaadinIcon.REFRESH, "Rotate immediately");
        rotateItem.setEnabled(isEnabled && hasRotation);
        rotateItem.addClickListener(e ->
                new RotateKeyConfirmDialog(parentView, objectService, keyId, this::refresh).open());

        // ── 4. Cancel deletion ────────────────────────────────────────────────
        MenuItem cancelItem = createMenuItem(contextMenu, VaadinIcon.REFRESH, "Cancel deletion");
        cancelItem.setEnabled(isPending);
        cancelItem.addClickListener(e -> cancelDeletion());

        // ── 5. Permanently delete ─────────────────────────────────────────────
        MenuItem deleteItem = createMenuItem(contextMenu, VaadinIcon.TRASH, "Permanently delete");
        deleteItem.setEnabled(isPending);
        deleteItem.getStyle().set("color", "var(--lumo-error-color)");
        deleteItem.addClickListener(e -> confirmPermanentDelete());
    }

    private MenuItem createMenuItem(ContextMenu menu, VaadinIcon icon, String label) {
        HorizontalLayout layout = new HorizontalLayout(icon.create(), new Span(label));
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        return menu.addItem(layout);
    }

    // ─── Action methods ──────────────────────────────────────────────────────

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
                metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED,
                this::refresh).open();
    }

    private void scheduleDeletion() {
        new ScheduleKeyDeletionDialog(parentView, objectService, keyId,
                metadata != null ? metadata.getPendingDeletionWindowDays() : null,
                this::refresh).open();
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(parentView, objectService, keyId, aliasOrId, this::refresh).open();
    }

    private void confirmPermanentDelete() {
        Runnable onConfirm = () -> getUI().ifPresent(ui -> ui.access(() ->
                getParent().ifPresent(p -> {
                    if (p instanceof VerticalLayout vl) vl.remove(this);
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

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCardAttach(AttachEvent event) {
        loadVersionCount();
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

    // ─── Extra CSS ────────────────────────────────────────────────────────────

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
                .key-card .meta-row .meta-value {
                    font-weight: 500;
                }
                .key-card .deletion-warning {
                    background: var(--lumo-error-color-10pct);
                    color: var(--lumo-error-text-color);
                    padding: var(--lumo-space-s);
                    border-radius: var(--lumo-border-radius-m);
                    display: flex;
                    align-items: center;
                    gap: var(--lumo-space-s);
                    margin-top: var(--lumo-space-xs);
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