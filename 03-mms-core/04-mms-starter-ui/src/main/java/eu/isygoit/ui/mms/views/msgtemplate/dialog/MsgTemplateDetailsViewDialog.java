package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Read-only details dialog for a {@link MsgTemplateDto}, organized into
 * titled sections (Identity, Configuration, Relations, Description, File,
 * Audit) with icon-labeled, vertically stacked fields — same shared
 * {@link DetailsViewDialog} convention used by
 * {@code ApplicationDetailsViewDialog}, {@code VCalendarDetailsViewDialog}
 * and the other {@code *DetailsViewDialog} classes across the app.
 */
@Slf4j
public class MsgTemplateDetailsViewDialog extends DetailsViewDialog {

    private final MsgTemplateFileService templateFileService;
    private final SenderConfigService senderConfigService;
    private final MsgTemplateDto template;
    private String senderConfigDisplayName;
    private String senderConfigTooltip;

    public MsgTemplateDetailsViewDialog(MsgTemplateFileService templateFileService,
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
        // ── Identity — name/code/tenant (text identifiers) ───────────────────
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("mms.msgtemplate.dialog.view.id"),
                template.getId() != null ? template.getId().toString() : null, true);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("mms.msgtemplate.dialog.view.code"),
                template.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("mms.msgtemplate.dialog.view.tenant"),
                template.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.FILE_TEXT, I18n.t("mms.msgtemplate.dialog.view.name"),
                template.getName());
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.identity"), identityGrid));

        // ── Configuration — language/default sender ──────────────────────────
        Div configGrid = createDetailGrid();
        addFieldToGrid(configGrid, VaadinIcon.FLAG, I18n.t("mms.msgtemplate.dialog.view.language"),
                template.getLanguage() != null ? template.getLanguage().name() : null);
        addFieldToGrid(configGrid, VaadinIcon.ENVELOPE_O, I18n.t("mms.msgtemplate.dialog.view.defaultSender"),
                template.getDefaultSender());
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.configuration"), configGrid));

        // ── Relations — sender config reference ──────────────────────────────
        Div relationsGrid = createDetailGrid();
        String senderConfigValue = senderConfigTooltip != null
                ? senderConfigDisplayName + " – " + senderConfigTooltip
                : senderConfigDisplayName;
        addFieldToGrid(relationsGrid, VaadinIcon.ENVELOPE, I18n.t("mms.msgtemplate.dialog.view.senderConfig"),
                senderConfigValue, true);
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.relations"), relationsGrid));

        // ── Description — free text, a full-width block rather than a grid
        //    field, since it doesn't fit the short label/value pattern well ──
        if (template.getDescription() != null && !template.getDescription().isBlank()) {
            Paragraph descParagraph = new Paragraph(template.getDescription());
            descParagraph.addClassName("wams-card__detail-field-value");
            add(createSection(I18n.t("mms.msgtemplate.dialog.view.description"), descParagraph));
        }

        // ── File ───────────────────────────────────────────────────────────
        Div fileGrid = createDetailGrid();
        addFieldToGrid(fileGrid, VaadinIcon.FILE, I18n.t("mms.msgtemplate.dialog.view.file"),
                template.getOriginalFileName());
        addFieldToGrid(fileGrid, VaadinIcon.FOLDER_OPEN, I18n.t("mms.msgtemplate.dialog.view.fileName"),
                template.getFileName());
        addFieldToGrid(fileGrid, VaadinIcon.ROAD, I18n.t("mms.msgtemplate.dialog.view.path"),
                template.getPath());

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

        // ── Audit — created/updated by & date ─────────────────────────────
        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("mms.msgtemplate.dialog.view.field.created"),
                template.getCreateDate() != null ? DateHelper.formatToHumanReadable(template.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("mms.msgtemplate.dialog.view.field.created.by"),
                template.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("mms.msgtemplate.dialog.view.field.updated"),
                template.getUpdateDate() != null ? DateHelper.formatToHumanReadable(template.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("mms.msgtemplate.dialog.view.field.updated.by"),
                template.getUpdatedBy());
        add(createSection(I18n.t("mms.msgtemplate.dialog.view.section.audit"), auditGrid));
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
