package eu.isygoit.ui.mms.views.msgtemplate;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.DeleteMsgTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditMsgTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditTemplateContentDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.MsgTemplateDetailsViewDialog;
import eu.isygoit.ui.mms.views.sender.dialog.SenderConfigDetailsViewDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
class MsgTemplateCard extends BaseCard<MsgTemplateManagementView, MsgTemplateService> {

    private final MsgTemplateDto template;
    private final MsgTemplateFileService templateFileService;
    private final SenderConfigService senderConfigService;
    private final Runnable onRefresh;

    // Cache for sender config details
    private final Map<Long, SenderConfigDto> senderConfigCache = new HashMap<>();

    // Dedicated body container – cleared and rebuilt on refresh
    private final VerticalLayout bodyContainer = new VerticalLayout();

    // UI components (updated on refresh)
    private Span titleSpan;
    private Span languageChip;
    private Span codeSpan;
    private Span descriptionSpan;
    private Span senderConfigSpan;
    private Button viewSenderConfigButton;
    private Button viewButton;
    private Button editButton;
    private Button deleteButton;
    private Button downloadButton;
    private Button editContentButton;

    public MsgTemplateCard(MsgTemplateManagementView parentView,
                           MsgTemplateService templateService,
                           MsgTemplateFileService templateFileService,
                           SenderConfigService senderConfigService,
                           MsgTemplateDto template,
                           Runnable onRefresh) {
        super(parentView, templateService);
        this.template = template;
        this.templateFileService = templateFileService;
        this.senderConfigService = senderConfigService;
        this.onRefresh = onRefresh;

        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.addClassName("template-card-body");
        add(bodyContainer);

        initCard();
        loadSenderConfigDetails();
    }

    // ─── Public Accessors ─────────────────────────────────────────────────────

    public MsgTemplateDto getTemplate() {
        return template;
    }

    public Long getTemplateId() {
        return template.getId();
    }

    public String getTemplateName() {
        return template.getName();
    }

    // ─── Load Sender Config Details ─────────────────────────────────────────

    private void loadSenderConfigDetails() {
        Long senderConfigId = template.getSenderConfigId();
        if (senderConfigId != null) {
            try {
                ResponseEntity<SenderConfigDto> response = senderConfigService.findById(senderConfigId);
                if (response.getBody() != null) {
                    senderConfigCache.put(senderConfigId, response.getBody());
                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to load sender config details for id {}", senderConfigId, e);
            }
        }
    }

    private String getSenderConfigDisplayName() {
        Long id = template.getSenderConfigId();
        if (id == null) {
            return I18n.t("mms.common.value.notAvailable");
        }
        SenderConfigDto config = senderConfigCache.get(id);
        if (config != null) {
            return config.getName() != null ? config.getName() : config.getCode();
        }
        return String.valueOf(id);
    }

    private String getSenderConfigTooltip() {
        Long id = template.getSenderConfigId();
        if (id == null) {
            return null;
        }
        SenderConfigDto config = senderConfigCache.get(id);
        if (config != null) {
            StringBuilder tooltip = new StringBuilder();
            if (config.getName() != null) {
                tooltip.append(config.getName());
            }
            if (config.getDescription() != null) {
                if (tooltip.length() > 0) {
                    tooltip.append(" - ");
                }
                tooltip.append(config.getDescription());
            }
            if (tooltip.length() == 0 && config.getCode() != null) {
                tooltip.append(config.getCode());
            }
            return tooltip.toString();
        }
        return String.valueOf(id);
    }

    private SenderConfigDto getSenderConfig() {
        Long id = template.getSenderConfigId();
        if (id == null) {
            return null;
        }
        return senderConfigCache.get(id);
    }

    // ─── Refresh – fully reloads the card ──────────────────────────────────

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<MsgTemplateDto> response = objectService.findById(template.getId());
                if (response.getBody() != null) {
                    MsgTemplateDto updatedTemplate = response.getBody();
                    template.setName(updatedTemplate.getName());
                    template.setCode(updatedTemplate.getCode());
                    template.setDescription(updatedTemplate.getDescription());
                    template.setLanguage(updatedTemplate.getLanguage());
                    template.setFileName(updatedTemplate.getFileName());
                    template.setOriginalFileName(updatedTemplate.getOriginalFileName());
                    template.setPath(updatedTemplate.getPath());
                    template.setDefaultSender(updatedTemplate.getDefaultSender());
                    template.setSenderConfigId(updatedTemplate.getSenderConfigId());

                    // Reload sender config details
                    senderConfigCache.clear();
                    loadSenderConfigDetails();
                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to refresh template card for {}", template.getId(), e);
            }
        }));
    }

    private void updateDisplay() {
        // Null check for all span components
        if (titleSpan == null) {
            titleSpan = buildTitleSpan("", "");
        }
        if (languageChip == null) {
            languageChip = buildStatusChip("", "");
        }
        if (codeSpan == null) {
            codeSpan = new Span();
        }
        if (descriptionSpan == null) {
            descriptionSpan = new Span();
        }
        if (senderConfigSpan == null) {
            senderConfigSpan = new Span();
        }

        String displayName = template.getName() != null ? template.getName() : I18n.t("mms.msgtemplate.card.fallback.name", template.getId());
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        // Language chip
        String language = template.getLanguage() != null ? template.getLanguage().name() : I18n.t("mms.common.value.notAvailable");
        languageChip.setText(language);
        applyChipColor(languageChip, getLanguageColor(template.getLanguage()));

        codeSpan.setText(template.getCode() != null ? template.getCode() : I18n.t("mms.common.value.notAvailable"));
        descriptionSpan.setText(template.getDescription() != null ? template.getDescription() : I18n.t("mms.msgtemplate.card.no.description"));

        // Sender Config with name and tooltip
        String senderConfigDisplay = getSenderConfigDisplayName();
        senderConfigSpan.setText(senderConfigDisplay);
        String tooltip = getSenderConfigTooltip();
        if (tooltip != null) {
            senderConfigSpan.getElement().setAttribute("title", tooltip);
        }

        // Update view sender config button
        boolean hasSenderConfig = template.getSenderConfigId() != null;
        if (viewSenderConfigButton != null) {
            viewSenderConfigButton.setEnabled(hasSenderConfig);
            viewSenderConfigButton.setTooltipText(hasSenderConfig ?
                    I18n.t("mms.msgtemplate.card.view.sender.config.tooltip") :
                    I18n.t("mms.msgtemplate.card.view.sender.config.disabled.tooltip"));
        }

        boolean hasFile = template.getFileName() != null && !template.getFileName().isEmpty();
        if (downloadButton != null) {
            downloadButton.setEnabled(hasFile);
            downloadButton.setTooltipText(hasFile ?
                    I18n.t("mms.msgtemplate.card.download.tooltip") :
                    I18n.t("mms.msgtemplate.card.download.disabled.tooltip"));
        }
        if (editContentButton != null) {
            editContentButton.setEnabled(hasFile);
            editContentButton.setTooltipText(hasFile ?
                    I18n.t("mms.msgtemplate.card.edit.content.tooltip") :
                    I18n.t("mms.msgtemplate.card.edit.content.disabled.tooltip"));
        }

        bodyContainer.removeAll();
        buildBodyRows();
    }

    private ChipColor getLanguageColor(IEnumLanguage.Types language) {
        if (language == null) return ChipColor.NEUTRAL;
        switch (language) {
            case EN:
                return ChipColor.INFO;
            case FR:
                return ChipColor.SUCCESS;
            case AR:
                return ChipColor.WARNING;
            default:
                return ChipColor.NEUTRAL;
        }
    }

    // ─── BaseCard Implementation ─────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "template-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.addClassName("wams-title-row");

        // Tenant badge
        if (template.getTenant() != null) {
            Span tenantBadge = new Span(template.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.addClassName("wams-tenant-badge");
            left.add(tenantBadge);
        }

        // Title
        String displayName = template.getName() != null ? template.getName() : I18n.t("mms.msgtemplate.card.fallback.name", template.getId());
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

        // Language chip
        String language = template.getLanguage() != null ? template.getLanguage().name() : I18n.t("mms.common.value.notAvailable");
        languageChip = buildStatusChip(language, I18n.t("mms.msgtemplate.card.language.tooltip", language));
        applyChipColor(languageChip, getLanguageColor(template.getLanguage()));
        left.add(languageChip);

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        List<Button> buttons = new ArrayList<>();
        boolean hasFile = template.getFileName() != null && !template.getFileName().isEmpty();

        // Details/View
        buttons.add(createDetailsButton(I18n.t("mms.msgtemplate.card.view.tooltip"), this::viewTemplate));

        // Edit (metadata)
        editButton = createEditButton(I18n.t("mms.msgtemplate.card.edit.tooltip"), this::openEditDialog);
        buttons.add(editButton);

        // Entity-specific extras: download, edit content
        downloadButton = createIconButton(VaadinIcon.DOWNLOAD, I18n.t("mms.msgtemplate.card.download.tooltip"));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        downloadButton.addClickListener(e -> downloadTemplate());
        downloadButton.setEnabled(hasFile);
        buttons.add(downloadButton);

        editContentButton = createIconButton(VaadinIcon.EDIT, I18n.t("mms.msgtemplate.card.edit.content.tooltip"));
        editContentButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        editContentButton.addClickListener(e -> openEditContentDialog());
        editContentButton.setEnabled(hasFile);
        buttons.add(editContentButton);

        // Delete – always last, always danger-styled
        deleteButton = createDeleteButton(I18n.t("mms.msgtemplate.card.delete.tooltip"), this::openDeleteDialog);
        buttons.add(deleteButton);

        return buttons;
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        // Essential fields only – quick-scan card. Everything else (tenant,
        // default sender, original file name, storage path, ...) lives in
        // MsgTemplateDetailsViewDialog, which shows every MsgTemplateDto field.

        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.CODE,
                I18n.t("mms.msgtemplate.card.code"),
                template.getCode() != null ? template.getCode() : I18n.t("mms.common.value.notAvailable"),
                template.getCode() != null ? template.getCode() : ""
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.FILE_TEXT,
                I18n.t("mms.msgtemplate.card.description"),
                template.getDescription() != null ? template.getDescription() : I18n.t("mms.msgtemplate.card.no.description")
        ));

        // Sender Config row with tooltip and view button
        String senderConfigDisplay = getSenderConfigDisplayName();
        String tooltip = getSenderConfigTooltip();
        SenderConfigDto senderConfig = getSenderConfig();

        // Create sender config row with custom value span
        Span valueSpan = new Span(senderConfigDisplay);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-value");
        if (tooltip != null) {
            valueSpan.getElement().setAttribute("title", tooltip);
        }

        // Create view sender config button (small, inline)
        viewSenderConfigButton = new Button(new Icon(VaadinIcon.EYE));
        viewSenderConfigButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        viewSenderConfigButton.setTooltipText(I18n.t("mms.msgtemplate.card.view.sender.config.tooltip"));
        boolean hasSenderConfig = template.getSenderConfigId() != null && senderConfig != null;
        viewSenderConfigButton.setEnabled(hasSenderConfig);
        viewSenderConfigButton.addClickListener(e -> viewSenderConfig());

        HorizontalLayout senderConfigRow = createDetailRow(
                VaadinIcon.SERVER,
                I18n.t("mms.msgtemplate.card.senderConfig"),
                valueSpan
        );

        // Add the view button to the sender config row
        senderConfigRow.add(viewSenderConfigButton);

        bodyContainer.add(senderConfigRow);
    }

    // ─── Helper Methods ──────────────────────────────────────────────────────

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-value");
        return createDetailRow(icon, label, valueSpan);
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, Span valueSpan) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("detail-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("detail-label");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createDetailRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createDetailRow(icon, label, value);

        if (copyValue != null && !copyValue.isEmpty()) {
            Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
            copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
            copyBtn.setTooltipText(I18n.t("mms.msgtemplate.card.copy.tooltip"));
            copyBtn.addClassName("detail-copy-button");
            copyBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.getPage().executeJs(
                        "navigator.clipboard.writeText($0)",
                        copyValue
                ));
            });
            row.add(copyBtn);
        }

        return row;
    }

    // ─── Action Methods ─────────────────────────────────────────────────────

    private void viewTemplate() {
        new MsgTemplateDetailsViewDialog(templateFileService, senderConfigService, template).open();
    }

    private void viewSenderConfig() {
        Long senderConfigId = template.getSenderConfigId();
        if (senderConfigId != null) {
            SenderConfigDto config = senderConfigCache.get(senderConfigId);
            if (config != null) {
                new SenderConfigDetailsViewDialog(config).open();
            } else {
                // Try to load it fresh
                try {
                    ResponseEntity<SenderConfigDto> response = senderConfigService.findById(senderConfigId);
                    if (response.getBody() != null) {
                        senderConfigCache.put(senderConfigId, response.getBody());
                        new SenderConfigDetailsViewDialog(response.getBody()).open();
                    } else {
                        Notification.show(I18n.t("mms.msgtemplate.card.view.sender.config.not.found"),
                                        3000, Notification.Position.BOTTOM_END)
                                .addThemeVariants(NotificationVariant.LUMO_WARNING);
                    }
                } catch (Exception e) {
                    log.error("Failed to load sender config for id {}", senderConfigId, e);
                    Notification.show(I18n.t("mms.msgtemplate.card.view.sender.config.error", e.getMessage()),
                                    5000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        }
    }

    private void downloadTemplate() {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            return;
        }
        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                Resource resource = response.getBody();
                byte[] content = resource.getInputStream().readAllBytes();
                String base64Content = java.util.Base64.getEncoder().encodeToString(content);
                String fileName = template.getOriginalFileName() != null ?
                        template.getOriginalFileName() : template.getFileName();

                getUI().ifPresent(ui -> ui.getPage().executeJs(
                        "const byteCharacters = atob($0);" +
                                "const byteNumbers = new Array(byteCharacters.length);" +
                                "for (let i = 0; i < byteCharacters.length; i++) {" +
                                "    byteNumbers[i] = byteCharacters.charCodeAt(i);" +
                                "}" +
                                "const byteArray = new Uint8Array(byteNumbers);" +
                                "const blob = new Blob([byteArray]);" +
                                "const url = URL.createObjectURL(blob);" +
                                "const a = document.createElement('a');" +
                                "a.href = url;" +
                                "a.download = $1;" +
                                "a.click();" +
                                "URL.revokeObjectURL(url);",
                        base64Content, fileName
                ));
            }
        } catch (Exception e) {
            log.error("Failed to download template file for {}", template.getId(), e);
            Notification.show(I18n.t("mms.msgtemplate.download.error", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void openEditContentDialog() {
        new EditTemplateContentDialog(parentView, objectService, templateFileService, template, () -> {
            refresh();
            if (onRefresh != null) {
                onRefresh.run();
            }
        }).open();
    }

    private void openEditDialog() {
        new EditMsgTemplateDialog(objectService, templateFileService, senderConfigService, template, () -> {
            refresh();
            if (onRefresh != null) {
                onRefresh.run();
            }
        }).open();
    }

    private void openDeleteDialog() {
        new DeleteMsgTemplateDialog(parentView, objectService, template, () -> {
            if (onRefresh != null) {
                onRefresh.run();
            }
            getParent().ifPresent(parent -> {
                if (parent instanceof VerticalLayout layout) {
                    layout.remove(this);
                }
            });
        }).open();
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void onCardAttach(AttachEvent event) {
        // No additional lifecycle actions needed
    }
}