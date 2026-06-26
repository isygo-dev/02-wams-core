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
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.DeleteMsgTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditMsgTemplateDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.EditTemplateContentDialog;
import eu.isygoit.ui.mms.views.msgtemplate.dialog.ViewMsgTemplateDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
class MsgTemplateCard extends BaseCard<MsgTemplateManagementView, MsgTemplateService> {

    private final MsgTemplateDto template;
    private final MsgTemplateFileService templateFileService;
    private final Runnable onRefresh;

    private final VerticalLayout bodyContainer = new VerticalLayout();

    private Span titleSpan;
    private Span languageChip;
    private Span codeSpan;
    private Span descriptionSpan;
    private Span fileNameSpan;
    private Button viewButton;
    private Button editButton;
    private Button deleteButton;
    private Button downloadButton;
    private Button editContentButton;

    public MsgTemplateCard(MsgTemplateManagementView parentView,
                           MsgTemplateService templateService,
                           MsgTemplateFileService templateFileService,
                           MsgTemplateDto template,
                           Runnable onRefresh) {
        super(parentView, templateService);
        this.template = template;
        this.templateFileService = templateFileService;
        this.onRefresh = onRefresh;

        bodyContainer.setPadding(false);
        bodyContainer.setSpacing(true);
        bodyContainer.setWidthFull();
        bodyContainer.addClassName("template-card-body");
        add(bodyContainer);

        initCard();
    }

    public MsgTemplateDto getTemplate() {
        return template;
    }

    public Long getTemplateId() {
        return template.getId();
    }

    public String getTemplateName() {
        return template.getName();
    }

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

                    updateDisplay();
                }
            } catch (Exception e) {
                log.error("Failed to refresh template card for {}", template.getId(), e);
            }
        }));
    }

    private void updateDisplay() {
        String displayName = template.getName() != null ? template.getName() : "Template " + template.getId();
        titleSpan.setText(displayName);
        titleSpan.getElement().setAttribute("title", displayName);

        // Language chip
        String language = template.getLanguage() != null ? template.getLanguage().name() : "N/A";
        languageChip.setText(language);
        ChipColor color = getLanguageColor(template.getLanguage());
        languageChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());

        codeSpan.setText(template.getCode() != null ? template.getCode() : "N/A");
        descriptionSpan.setText(template.getDescription() != null ? template.getDescription() : I18n.t("template.card.no.description"));
        fileNameSpan.setText(template.getOriginalFileName() != null ? template.getOriginalFileName() : I18n.t("template.card.no.file"));

        boolean hasFile = template.getFileName() != null && !template.getFileName().isEmpty();
        downloadButton.setEnabled(hasFile);
        downloadButton.setTooltipText(hasFile ?
                I18n.t("template.card.download.tooltip") :
                I18n.t("template.card.download.disabled.tooltip"));
        editContentButton.setEnabled(hasFile);
        editContentButton.setTooltipText(hasFile ?
                I18n.t("template.card.edit.content.tooltip") :
                I18n.t("template.card.edit.content.disabled.tooltip"));

        bodyContainer.removeAll();
        buildBodyRows();
    }

    private ChipColor getLanguageColor(IEnumLanguage.Types language) {
        if (language == null) return ChipColor.NEUTRAL;
        switch (language) {
            case EN: return ChipColor.INFO;
            case FR: return ChipColor.SUCCESS;
            case AR: return ChipColor.WARNING;
            default: return ChipColor.NEUTRAL;
        }
    }

    @Override
    protected String cardCssClassName() {
        return "template-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

        // Tenant badge
        if (template.getTenant() != null) {
            Span tenantBadge = new Span(template.getTenant());
            tenantBadge.addClassName(LumoUtility.Background.CONTRAST_5);
            tenantBadge.addClassName(LumoUtility.Padding.XSMALL);
            tenantBadge.addClassName(LumoUtility.BorderRadius.SMALL);
            tenantBadge.addClassName(LumoUtility.FontSize.XSMALL);
            tenantBadge.getStyle().set("font-family", "monospace");
            left.add(tenantBadge);
        }

        // Title
        String displayName = template.getName() != null ? template.getName() : "Template " + template.getId();
        titleSpan = buildTitleSpan(displayName, displayName);
        left.add(titleSpan);

        // Language chip
        String language = template.getLanguage() != null ? template.getLanguage().name() : "N/A";
        languageChip = buildStatusChip(language, "Language: " + language);
        ChipColor color = getLanguageColor(template.getLanguage());
        languageChip.getStyle()
                .set("background-color", color.background())
                .set("color", color.foreground());
        left.add(languageChip);

        // ID badge
        Span idBadge = new Span("ID: " + template.getId());
        idBadge.addClassName(LumoUtility.FontSize.XSMALL);
        idBadge.addClassName(LumoUtility.TextColor.TERTIARY);
        idBadge.getStyle().set("font-family", "monospace");
        left.add(idBadge);

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        List<Button> buttons = new ArrayList<>();

        // View Button
        Button viewBtn = createIconButton(VaadinIcon.EYE, I18n.t("template.card.view.tooltip"));
        viewBtn.addClickListener(e -> viewTemplate());
        buttons.add(viewBtn);

        // Download Button
        downloadButton = createIconButton(VaadinIcon.DOWNLOAD, I18n.t("template.card.download.tooltip"));
        downloadButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        downloadButton.addClickListener(e -> downloadTemplate());
        boolean hasFile = template.getFileName() != null && !template.getFileName().isEmpty();
        downloadButton.setEnabled(hasFile);
        buttons.add(downloadButton);

        // Edit Content Button - for online editing
        editContentButton = createIconButton(VaadinIcon.EDIT, I18n.t("template.card.edit.content.tooltip"));
        editContentButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        editContentButton.addClickListener(e -> openEditContentDialog());
        editContentButton.setEnabled(hasFile);
        buttons.add(editContentButton);

        // Edit Button (metadata)
        editButton = createIconButton(VaadinIcon.PENCIL, I18n.t("template.card.edit.tooltip"));
        editButton.addClickListener(e -> openEditDialog());
        buttons.add(editButton);

        // Delete Button
        deleteButton = createIconButton(VaadinIcon.TRASH, I18n.t("template.card.delete.tooltip"));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClickListener(e -> openDeleteDialog());
        buttons.add(deleteButton);

        return buttons;
    }

    @Override
    protected void buildBodyRows() {
        bodyContainer.removeAll();

        bodyContainer.add(createDetailRow(
                VaadinIcon.GLOBE,
                I18n.t("template.card.tenant"),
                template.getTenant() != null ? template.getTenant() : "N/A"
        ));

        bodyContainer.add(createDetailRowWithCopy(
                VaadinIcon.CODE,
                I18n.t("template.card.code"),
                template.getCode() != null ? template.getCode() : "N/A",
                template.getCode() != null ? template.getCode() : ""
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.FILE_TEXT,
                I18n.t("template.card.description"),
                template.getDescription() != null ? template.getDescription() : I18n.t("template.card.no.description")
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.LOCATION_ARROW,
                I18n.t("template.card.language"),
                template.getLanguage() != null ? template.getLanguage().name() : "N/A"
        ));

        bodyContainer.add(createDetailRow(
                VaadinIcon.FILE,
                I18n.t("template.card.file"),
                template.getOriginalFileName() != null ? template.getOriginalFileName() : I18n.t("template.card.no.file")
        ));

        if (template.getFileName() != null) {
            bodyContainer.add(createDetailRow(
                    VaadinIcon.FOLDER,
                    I18n.t("template.card.path"),
                    template.getPath() != null ? template.getPath() : "N/A"
            ));
        }
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");
        return createDetailRow(icon, label, valueSpan);
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, Span valueSpan) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("detail-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "120px");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createDetailRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = createDetailRow(icon, label, value);

        if (copyValue != null && !copyValue.isEmpty()) {
            Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
            copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
            copyBtn.setTooltipText(I18n.t("template.card.copy.tooltip"));
            copyBtn.addClickListener(e -> {
                getUI().ifPresent(ui -> ui.getPage().executeJs(
                        "navigator.clipboard.writeText($0)",
                        copyValue
                ));
            });
            copyBtn.getStyle().set("padding", "var(--lumo-space-xs)");
            row.add(copyBtn);
        }

        return row;
    }

    private void viewTemplate() {
        new ViewMsgTemplateDialog(templateFileService, template).open();
    }

    private void downloadTemplate() {
        if (template.getFileName() == null || template.getFileName().isEmpty()) {
            return;
        }
        try {
            ResponseEntity<Resource> response = templateFileService.downloadFile(template.getId(), 0L);
            if (response.getBody() != null) {
                Resource resource = response.getBody();
                // Read the resource content as bytes
                byte[] content = resource.getInputStream().readAllBytes();
                // Encode to Base64
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
            Notification.show(I18n.t("template.download.error", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
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
        new EditMsgTemplateDialog(objectService, templateFileService, template, () -> {
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

    @Override
    protected void onCardAttach(AttachEvent event) {
        // No additional lifecycle actions needed
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .template-card .detail-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .template-card .detail-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .template-card .detail-row {
                        flex-wrap: wrap;
                    }
                    .template-card .detail-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}