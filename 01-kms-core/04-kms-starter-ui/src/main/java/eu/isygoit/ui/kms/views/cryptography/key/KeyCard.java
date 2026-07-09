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
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.i18n.I18n;
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

                    statusChip.setText(statusText);
                    applyChipColor(statusChip, ChipColor.fromStatus(statusText));
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
        left.addClassName("key-card__title-row");

        titleSpan = buildTitleSpan(aliasOrId, aliasOrId);
        statusChip = buildStatusChip(statusText, statusText);

        versionSpan = new Span();
        updateVersionDisplay();
        String currentVer = metadata != null && metadata.getCurrentVersion() != null
                ? metadata.getCurrentVersion() : "N/A";
        Button copyVersionBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, currentVer, I18n.t("kms.key.card.copy.version.tooltip"));
        copyVersionBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout versionLayout = new HorizontalLayout(versionSpan, copyVersionBtn);
        versionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        versionLayout.setSpacing(false);

        left.add(titleSpan, statusChip, versionLayout);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("kms.key.card.edit.tooltip"));
        editBtn.addClickListener(e -> updateKey());

        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("kms.key.card.describe.tooltip"));
        describeBtn.addClickListener(e -> describeKey());

        rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, "");
        rotationBtn.addClickListener(e -> toggleRotation());
        updateRotationButton();

        versionsBtn = createIconButton(VaadinIcon.CUBE, I18n.t("kms.key.card.versions.tooltip"));
        versionsBtn.addClickListener(e -> showVersionsDialog());

        moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V, I18n.t("kms.key.card.more.tooltip"));
        attachContextMenu(moreBtn);

        return List.of(editBtn, describeBtn, rotationBtn, versionsBtn, moreBtn);
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Description (with icon) — kept short for quick scanning
        bodyContainer.add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("kms.key.card.description"),
                metadata != null && metadata.getDescription() != null ? metadata.getDescription() : I18n.t("kms.key.card.no.description")));

        // Deletion warning — operationally important, stays visible on the card
        createDeletionWarningSpan();
        updateDeletionWarning();
        bodyContainer.add(deletionWarningSpan);
    }

    // ─── Helper row builders ─────────────────────────────────────────────────

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("key-card__row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("key-card__row-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("key-card__row-value");
        valueSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    // ─── Internal update helpers ─────────────────────────────────────────────

    private void updateDerivedFields() {
        aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                ? metadata.getKeyAlias() : keyId;
        statusText = (metadata != null && metadata.getKeyStatus() != null)
                ? metadata.getKeyStatus().name() : I18n.t("kms.key.card.status.unknown");
    }

    private void updateVersionDisplay() {
        String full = (metadata != null && metadata.getCurrentVersion() != null)
                ? metadata.getCurrentVersion() : "N/A";
        String display = full.length() > 12 ? full.substring(0, 12) + "…" : full;
        versionSpan.setText(display);
        versionSpan.addClassName(LumoUtility.FontSize.XSMALL);
        versionSpan.addClassName(LumoUtility.TextColor.TERTIARY);
        versionSpan.addClassName("key-card__version-span");
        versionSpan.getElement().setAttribute("title", I18n.t("kms.key.card.version.tooltip.current", full));
    }

    private void updateRotationButton() {
        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        boolean isPending = "PENDING_DELETION".equalsIgnoreCase(statusText);
        boolean isActive = isEnabled && !isPending;

        boolean rotationOn = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());

        if (rotationOn) {
            rotationBtn.setTooltipText(isActive ? I18n.t("kms.key.card.rotation.tooltip.disable.active") : I18n.t("kms.key.card.rotation.tooltip.disable.inactive"));
            rotationBtn.getStyle().set("color", isActive ? "var(--lumo-success-color)" : "var(--lumo-tertiary-text-color)");
        } else {
            rotationBtn.setTooltipText(isActive ? I18n.t("kms.key.card.rotation.tooltip.enable.active") : I18n.t("kms.key.card.rotation.tooltip.enable.inactive"));
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
        deletionWarningSpan.addClassName("wams-card__deletion-warning");
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
        if (metadata == null) return I18n.t("kms.key.card.deletion.warning.default");
        LocalDateTime deletionDate = metadata.getDeletionDate();
        if (deletionDate != null) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), deletionDate);
            if (daysRemaining > 0) return I18n.t("kms.key.card.deletion.warning.days", daysRemaining);
            if (daysRemaining == 0) return I18n.t("kms.key.card.deletion.warning.today");
            return I18n.t("kms.key.card.deletion.warning.overdue");
        }
        return I18n.t("kms.key.card.deletion.warning.default");
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
        String toggleLabel = isEnabled ? I18n.t("kms.key.card.menu.toggle.disable") : I18n.t("kms.key.card.menu.toggle.enable");
        VaadinIcon toggleIcon = isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK;
        MenuItem toggleItem = createMenuItem(contextMenu, toggleIcon, toggleLabel);
        toggleItem.setEnabled(!isPending);
        toggleItem.addClickListener(e -> toggleKeyStatus());

        // ── 2. Schedule deletion ──────────────────────────────────────────────
        MenuItem scheduleItem = createMenuItem(contextMenu, VaadinIcon.CLOCK, I18n.t("kms.key.card.menu.schedule.delete"));
        scheduleItem.setEnabled(!isPending);
        scheduleItem.addClickListener(e -> scheduleDeletion());

        // ── 3. Rotate immediately ─────────────────────────────────────────────
        MenuItem rotateItem = createMenuItem(contextMenu, VaadinIcon.REFRESH, I18n.t("kms.key.card.menu.rotate.now"));
        rotateItem.setEnabled(isEnabled && hasRotation);
        rotateItem.addClickListener(e ->
                new RotateKeyConfirmDialog(parentView, objectService, keyId, this::refresh).open());

        // ── 4. Cancel deletion ────────────────────────────────────────────────
        MenuItem cancelItem = createMenuItem(contextMenu, VaadinIcon.REFRESH, I18n.t("kms.key.card.menu.cancel.deletion"));
        cancelItem.setEnabled(isPending);
        cancelItem.addClickListener(e -> cancelDeletion());

        // ── 5. Permanently delete ─────────────────────────────────────────────
        MenuItem deleteItem = createMenuItem(contextMenu, VaadinIcon.TRASH, I18n.t("kms.key.card.menu.permanent.delete"));
        deleteItem.setEnabled(isPending);
        deleteItem.addClassName("key-card__menu-item--danger");
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
            versionsBtn.setText(I18n.t("kms.key.card.versions.count", versionCount));
            versionsBtn.setTooltipText(I18n.t("kms.key.card.versions.total", versionCount));
        }));
    }
}