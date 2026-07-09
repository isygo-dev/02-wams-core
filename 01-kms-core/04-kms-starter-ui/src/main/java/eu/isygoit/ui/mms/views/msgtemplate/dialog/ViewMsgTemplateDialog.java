package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
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
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Read-only details dialog for a {@link MsgTemplateDto}, split into sections
 * by data type (Identity, Configuration, Relations, File) with a divider
 * between each — same convention as {@code ParameterDetailsDialog} and other
 * {@code *DetailsDialog} classes across the app.
 */
@Slf4j
public class ViewMsgTemplateDialog extends NoActionDialog {

    private final MsgTemplateFileService templateFileService;
    private final SenderConfigService senderConfigService;
    private final MsgTemplateDto template;
    private String senderConfigDisplayName;
    private String senderConfigTooltip;

    public ViewMsgTemplateDialog(MsgTemplateFileService templateFileService,
                                 SenderConfigService senderConfigService,
                                 MsgTemplateDto template) {
        super(I18n.t("mms.msgtemplate.dialog.view.title",
                template.getName() != null ? template.getName() : template.getId()));
        this.templateFileService = templateFileService;
        this.senderConfigService = senderConfigService;
        this.template = template;

        setWidth("600px");
        setMaxWidth("95vw");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("msgtemplate-details-dialog");

        loadSenderConfigDetails();
        buildContent();
    }

    private void loadSenderConfigDetails() {
        Long senderConfigId = template.getSenderConfigId();
        if (senderConfigId != null) {
            try {
                ResponseEntity<SenderConfigDto> response = senderConfigService.findById(senderConfigId);
                if (response.getBody() != null) {
                    SenderConfigDto config = response.getBody();
                    senderConfigDisplayName = config.getName() != null ? config.getName() : config.getCode();
                    if (config.getDescription() != null) {
                        senderConfigTooltip = config.getDescription();
                    }
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to load sender config details for id {}", senderConfigId, e);
            }
        }
        senderConfigDisplayName = senderConfigId != null ?
                String.valueOf(senderConfigId) : I18n.t("mms.common.value.notAvailable");
        senderConfigTooltip = null;
    }

    private void buildContent() {
        // ── Identity ─────────────────────────────────────────────────────────
        Div identityGrid = new Div();
        identityGrid.addClassName("wams-view-details");
        addDetailRow(identityGrid, I18n.t("mms.msgtemplate.dialog.view.id"), template.getId().toString());
        addDetailRow(identityGrid, I18n.t("mms.msgtemplate.dialog.view.code"), template.getCode());
        addDetailRow(identityGrid, I18n.t("mms.msgtemplate.dialog.view.tenant"), template.getTenant());
        addDetailRow(identityGrid, I18n.t("mms.msgtemplate.dialog.view.name"), template.getName());
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.identity"), identityGrid));

        // ── Configuration ────────────────────────────────────────────────────
        Div configGrid = new Div();
        configGrid.addClassName("wams-view-details");
        addDetailRow(configGrid, I18n.t("mms.msgtemplate.dialog.view.language"),
                template.getLanguage() != null ? template.getLanguage().name() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(configGrid, I18n.t("mms.msgtemplate.dialog.view.defaultSender"),
                template.getDefaultSender() != null ? template.getDefaultSender() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(configGrid, I18n.t("mms.msgtemplate.dialog.view.description"),
                template.getDescription() != null ? template.getDescription() : I18n.t("mms.common.value.notAvailable"));
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.configuration"), configGrid));

        // ── Relations (sender config reference) ─────────────────────────────
        Div relationsGrid = new Div();
        relationsGrid.addClassName("wams-view-details");
        addDetailRowWithTooltip(relationsGrid, I18n.t("mms.msgtemplate.dialog.view.senderConfig"),
                senderConfigDisplayName, senderConfigTooltip);
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.relations"), relationsGrid));

        // ── File ─────────────────────────────────────────────────────────────
        Div fileGrid = new Div();
        fileGrid.addClassName("wams-view-details");
        addDetailRow(fileGrid, I18n.t("mms.msgtemplate.dialog.view.file"),
                template.getOriginalFileName() != null ? template.getOriginalFileName() : I18n.t("mms.common.value.notAvailable"));
        addDetailRow(fileGrid, I18n.t("mms.msgtemplate.dialog.view.fileName"),
                template.getFileName() != null ? template.getFileName() : I18n.t("mms.common.value.notAvailable"));
        if (template.getPath() != null) {
            addDetailRow(fileGrid, I18n.t("mms.msgtemplate.dialog.view.path"), template.getPath());
        }

        boolean hasFile = template.getFileName() != null && !template.getFileName().isEmpty();
        HorizontalLayout downloadRow = new HorizontalLayout();
        downloadRow.setWidthFull();
        downloadRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        Button downloadBtn = new Button(I18n.t("mms.msgtemplate.dialog.view.download"), new Icon(VaadinIcon.DOWNLOAD));
        downloadBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        downloadBtn.setEnabled(hasFile);
        downloadBtn.addClickListener(e -> downloadTemplate());
        downloadRow.add(downloadBtn);

        VerticalLayout fileSection = new VerticalLayout(fileGrid, downloadRow);
        fileSection.setPadding(false);
        fileSection.setSpacing(true);
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.file"), fileSection));
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
    }

    private void addDetailRow(Div container, String label, String value) {
        Div row = new Div();
        row.addClassName("wams-view-detail-row");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName("wams-view-detail-label");

        Span valueSpan = new Span(value != null ? value : I18n.t("mms.common.value.notAvailable"));
        valueSpan.addClassName("wams-view-detail-value");

        row.add(labelSpan, valueSpan);
        container.add(row);
    }

    private void addDetailRowWithTooltip(Div container, String label, String value, String tooltip) {
        Div row = new Div();
        row.addClassName("wams-view-detail-row");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName("wams-view-detail-label");

        Span valueSpan = new Span(value != null ? value : I18n.t("mms.common.value.notAvailable"));
        valueSpan.addClassName("wams-view-detail-value");
        if (tooltip != null && !tooltip.isEmpty()) {
            valueSpan.getElement().setAttribute("title", tooltip);
            // Add a small info icon to indicate there's more info
            Icon infoIcon = VaadinIcon.INFO_CIRCLE.create();
            infoIcon.setSize("14px");
            infoIcon.addClassName("wams-tooltip-icon");
            infoIcon.getElement().setAttribute("title", tooltip);
            row.add(labelSpan, valueSpan, infoIcon);
            container.add(row);
            return;
        }

        row.add(labelSpan, valueSpan);
        container.add(row);
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
            Notification.show(I18n.t("mms.msgtemplate.download.error", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
