package eu.isygoit.ui.views.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.ListKeyVersionsResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.AbstractKmsCard;
import eu.isygoit.ui.views.key.dialog.*;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class KeyCard extends AbstractKmsCard<KeyManagementView> {

    private final String keyId;
    private final ObjectMapper objectMapper;

    private DescribeKeyResponse.KeyMetadata metadata;
    private String aliasOrId;
    private String statusText;

    // Mutable UI refs for in-place refresh
    private Span titleSpan;
    private Span statusChip;
    private Span versionSpan;
    private Span descSpan;
    private HorizontalLayout metaRow1;
    private HorizontalLayout metaRow2;
    private Button rotationBtn;
    private Button versionsBtn;
    private Button moreBtn;
    private int versionCount = 0;

    // Context menu reference to avoid duplicate attachments
    private ContextMenu currentContextMenu;

    // ── Constructor ───────────────────────────────────────────────────────────

    KeyCard(KeyManagementView keyManagementView,
            KmsApiService kmsApiService,
            ObjectMapper objectMapper,
            String keyId,
            DescribeKeyResponse.KeyMetadata metadata) {
        super(keyManagementView, kmsApiService);
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.metadata = metadata;
        updateDerivedFields();
        initCard();          // assembles shell + header + body via template methods
    }

    // ── Public accessors ──────────────────────────────────────────────────────

    public String getKeyId() {
        return keyId;
    }

    public String getAliasOrId() {
        return aliasOrId;
    }

    public IEnumKeyStatus.Types getStatus() {
        return metadata != null ? metadata.getKeyStatus() : null;
    }

    // ── AbstractKmsCard contract ──────────────────────────────────────────────

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

        HorizontalLayout versionLayout = new HorizontalLayout(versionSpan, copyVersionBtn);
        versionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        versionLayout.setSpacing(false);

        left.add(titleSpan, statusChip, versionLayout);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias, description, tags, and rotation settings");
        editBtn.addClickListener(e -> updateKey());

        Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full key details");
        describeBtn.addClickListener(e -> describeKey());

        rotationBtn = createIconButton(VaadinIcon.ROTATE_RIGHT, "");
        rotationBtn.addClickListener(e -> toggleRotation());
        updateRotationButton();

        versionsBtn = createIconButton(VaadinIcon.CUBE, "View all key versions");
        versionsBtn.setText("Ver (...)"); // will be updated later
        versionsBtn.addClickListener(e -> showVersionsDialog());

        moreBtn = createIconButton(VaadinIcon.ELLIPSIS_DOTS_V,
                "More actions (enable/disable, schedule deletion, rotate immediately, etc.)");
        attachContextMenu(moreBtn);

        return List.of(editBtn, describeBtn, rotationBtn, versionsBtn, moreBtn);
    }

    @Override
    protected void buildBodyRows() {
        // Description
        descSpan = new Span();
        updateDescription();
        add(descSpan);

        // Meta row 1: spec, usage, created, region
        metaRow1 = new HorizontalLayout();
        metaRow1.setSpacing(true);
        metaRow1.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow1.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow1.getStyle().set("margin-top", "var(--lumo-space-s)").set("flex-wrap", "wrap");
        updateMetaRow1();
        add(metaRow1);

        // Meta row 2: ID (with copy), origin, rotation
        metaRow2 = new HorizontalLayout();
        metaRow2.setSpacing(true);
        metaRow2.addClassName(LumoUtility.FontSize.XSMALL);
        metaRow2.addClassName(LumoUtility.TextColor.TERTIARY);
        metaRow2.getStyle().set("margin-top", "var(--lumo-space-xs)").set("flex-wrap", "wrap");
        updateMetaRow2();
        add(metaRow2);
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        UI.getCurrent().getPage().addBrowserWindowResizeListener(e -> adjustForScreenWidth(e.getWidth()));
        UI.getCurrent().getPage().executeJs("return window.innerWidth")
                .then(Integer.class, this::adjustForScreenWidth);
        loadVersionCount();
    }

    // ── Refresh / in-place update ─────────────────────────────────────────────

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
                if (response.getBody() != null && response.getBody().getKeyMetadata() != null) {
                    this.metadata = response.getBody().getKeyMetadata();
                    updateDerivedFields();
                    titleSpan.setText(aliasOrId);
                    titleSpan.getElement().setAttribute("title", aliasOrId);
                    // Re-apply chip colour
                    ChipColor color = ChipColor.fromStatus(statusText);
                    statusChip.setText(statusText);
                    statusChip.getStyle()
                            .set("background-color", color.background())
                            .set("color", color.foreground());
                    statusChip.getElement().setAttribute("title", statusText);
                    updateVersionDisplay();
                    updateDescription();
                    updateMetaRow1();
                    updateMetaRow2();
                    updateRotationButton();
                    loadVersionCount();
                    // Refresh the context menu to reflect updated key state
                    attachContextMenu(moreBtn);
                }
            } catch (Exception e) {
                log.error("Failed to refresh key card for {}", keyId, e);
            }
        }));
    }

    // ── Context menu helper (refreshes on every call) ────────────────────────

    private void attachContextMenu(Button button) {
        // Remove existing context menu if any
        if (currentContextMenu != null) {
            currentContextMenu.setTarget(null); // detach from button
            currentContextMenu.removeAll(); // clear items
        }
        // Create new context menu
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.setTarget(button);
        contextMenu.setOpenOnClick(true);
        populateContextMenu(contextMenu);
        currentContextMenu = contextMenu;
    }

    private void populateContextMenu(ContextMenu contextMenu) {
        // Clear any existing items (double safety)
        contextMenu.getItems().clear();

        // ---------- Enable / Disable key ----------
        boolean isEnabled = metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED;
        String toggleLabel = isEnabled ? "Disable key" : "Enable key";
        VaadinIcon toggleIcon = isEnabled ? VaadinIcon.UNLOCK : VaadinIcon.LOCK;
        HorizontalLayout toggleItem = new HorizontalLayout(toggleIcon.create(), new Span(toggleLabel));
        toggleItem.setSpacing(true);
        toggleItem.setAlignItems(FlexComponent.Alignment.CENTER);
        contextMenu.addItem(toggleItem, e -> toggleKeyStatus());

        // ---------- Schedule deletion ----------
        HorizontalLayout scheduleItem = new HorizontalLayout(VaadinIcon.CLOCK.create(), new Span("Schedule deletion"));
        scheduleItem.setSpacing(true);
        scheduleItem.setAlignItems(FlexComponent.Alignment.CENTER);
        contextMenu.addItem(scheduleItem, e -> scheduleDeletion());

        // ---------- Rotate immediately (only if rotation enabled) ----------
        if (metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled())) {
            HorizontalLayout rotateItem = new HorizontalLayout(VaadinIcon.REFRESH.create(), new Span("Rotate immediately"));
            rotateItem.setSpacing(true);
            rotateItem.setAlignItems(FlexComponent.Alignment.CENTER);
            contextMenu.addItem(rotateItem, e ->
                    new RotateKeyConfirmDialog(parentView, kmsApiService, keyId, this::refresh).open());
        }

        // ---------- Cancellation / permanent deletion (only if pending) ----------
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
            // Non‑interactive item (disabled look)
            HorizontalLayout disabledItem = new HorizontalLayout(VaadinIcon.BAN.create(), new Span("Permanently delete (not pending)"));
            disabledItem.setSpacing(true);
            disabledItem.setAlignItems(FlexComponent.Alignment.CENTER);
            disabledItem.getStyle().set("opacity", "0.5");
            contextMenu.addItem(disabledItem, e -> {}); // empty listener does nothing
        }
    }

    // ── Derived-field helpers ─────────────────────────────────────────────────

    private void updateDerivedFields() {
        this.aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                ? metadata.getKeyAlias() : keyId;
        this.statusText = (metadata != null && metadata.getKeyStatus() != null)
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

    private void updateDescription() {
        String text = (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isEmpty())
                ? metadata.getDescription() : "No description provided";
        descSpan.setText(text);
        descSpan.addClassName(LumoUtility.FontSize.SMALL);
        descSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        descSpan.getStyle().set("margin-top", "var(--lumo-space-xs)")
                .set("display", "block").set("word-break", "break-word");
        descSpan.getElement().setAttribute("title", text);
    }

    private void updateMetaRow1() {
        metaRow1.removeAll();
        String keySpec = metadata != null && metadata.getKeySpec() != null ? metadata.getKeySpec().name() : "N/A";
        String keyUsage = metadata != null && metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : "N/A";
        String created = metadata != null && metadata.getCreateDate() != null
                ? metadata.getCreateDate().toLocalDate().toString() : "Unknown";
        String region = metadata != null && Boolean.TRUE.equals(metadata.getMultiRegion())
                ? "🌍 Multi-region" : "📍 Single-region";

        Span specSpan = new Span("Spec: " + keySpec);
        Span usageSpan = new Span("Usage: " + keyUsage);
        Span createdSpan = new Span("Created: " + created);
        Span regionSpan = new Span(region);

        specSpan.getElement().setAttribute("title", keySpec);
        usageSpan.getElement().setAttribute("title", keyUsage);
        createdSpan.getElement().setAttribute("title", created);
        regionSpan.getElement().setAttribute("title", region);

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
            keyIdLayout.add(keyIdSpan, MainView.createCopyButton(VaadinIcon.COPY, keyId, "Copy full key ID"));
            metaRow2.add(keyIdLayout, new Span("•"));
        }
        String origin = metadata != null && metadata.getOrigin() != null ? metadata.getOrigin().name() : "N/A";
        String rotation = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled())
                ? "✅ Rotation ON (" + metadata.getRotationPeriodInDays() + " Days)" : "❌ Rotation OFF";

        Span originSpan = new Span("Origin: " + origin);
        Span rotationSpan = new Span(rotation);
        originSpan.getElement().setAttribute("title", origin);
        rotationSpan.getElement().setAttribute("title", rotation);

        metaRow2.add(originSpan, new Span("•"), rotationSpan);
    }

    private void updateRotationButton() {
        boolean on = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        rotationBtn.setTooltipText(on ? "Disable automatic rotation" : "Enable automatic rotation");
        rotationBtn.getStyle().set("color", on
                ? "var(--lumo-success-color)"
                : "var(--lumo-tertiary-text-color)");
    }

    private void loadVersionCount() {
        try {
            ResponseEntity<ListKeyVersionsResponse> resp = kmsApiService.listKeyVersions(keyId, 100, null);
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

    // ── Responsive layout ─────────────────────────────────────────────────────

    private void adjustForScreenWidth(int width) {
        boolean mobile = width < 768;
        if (mobile) {
            headerRow.getStyle().set("flex-direction", "column");
            headerRow.setAlignItems(FlexComponent.Alignment.START);
            headerRow.setSpacing(false);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
            buttonBar.getStyle().set("margin-top", "var(--lumo-space-s)");
            metaRow1.setSpacing(false);
            metaRow2.setSpacing(false);
            metaRow1.addClassName(LumoUtility.FontSize.XXSMALL);
            metaRow2.addClassName(LumoUtility.FontSize.XXSMALL);
            metaRow1.getStyle().set("margin-bottom", "4px");
        } else {
            headerRow.getStyle().remove("flex-direction");
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
            headerRow.setSpacing(true);
            buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttonBar.getStyle().remove("margin-top");
            metaRow1.setSpacing(true);
            metaRow2.setSpacing(true);
            metaRow1.removeClassName(LumoUtility.FontSize.XXSMALL);
            metaRow2.removeClassName(LumoUtility.FontSize.XXSMALL);
            metaRow1.getStyle().remove("margin-bottom");
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void updateKey() {
        List<ListResourceTagsResponse.Tag> tags = fetchKeyTags();
        boolean rotationEnabled = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        Integer rotationPeriod = metadata != null ? metadata.getRotationPeriodInDays() : null;
        new UpdateKeyDialog(parentView, kmsApiService, objectMapper, keyId,
                metadata != null ? metadata.getKeyAlias() : null,
                metadata != null ? metadata.getDescription() : null,
                tags, rotationEnabled, rotationPeriod, this::refresh).open();
    }

    private void describeKey() {
        new DescribeKeyDialog(parentView, kmsApiService, objectMapper, keyId, metadata).open();
    }

    private void toggleRotation() {
        boolean currentlyEnabled = metadata != null && Boolean.TRUE.equals(metadata.getRotationEnabled());
        Integer currentPeriod = metadata != null ? metadata.getRotationPeriodInDays() : null;
        new ToggleRotationDialog(parentView, kmsApiService, keyId, currentlyEnabled, currentPeriod, this::refresh).open();
    }

    private void showVersionsDialog() {
        new ShowKeyVersionsDialog(kmsApiService, keyId, aliasOrId).open();
    }

    private void toggleKeyStatus() {
        new ToggleKeyStatusDialog(parentView, kmsApiService, keyId,
                metadata != null && metadata.getKeyStatus() == IEnumKeyStatus.Types.ENABLED, this::refresh).open();
    }

    private void scheduleDeletion() {
        new ScheduleKeyDeletionDialog(parentView, kmsApiService, keyId, metadata.getPendingDeletionWindowDays(), this::refresh).open();
    }

    private void cancelDeletion() {
        new CancelKeyDeletionDialog(parentView, kmsApiService, keyId, aliasOrId, this::refresh).open();
    }

    private void confirmPermanentDelete() {
        Runnable onConfirm = () -> getUI().ifPresent(ui -> ui.access(() ->
                getParent().ifPresent(p -> {
                    if (p instanceof com.vaadin.flow.component.orderedlayout.VerticalLayout vl) vl.remove(this);
                })
        ));
        new PermanentDeleteKeyDialog(parentView, kmsApiService, keyId, onConfirm).open();
    }

    private List<ListResourceTagsResponse.Tag> fetchKeyTags() {
        try {
            ResponseEntity<eu.isygoit.dto.KmsDtos.ListResourceTagsResponse> resp =
                    kmsApiService.listResourceTags(keyId, 100, null);
            if (resp.getBody() != null && resp.getBody().getTags() != null) return resp.getBody().getTags();
        } catch (Exception e) { /* ignore */ }
        return new ArrayList<>();
    }
}