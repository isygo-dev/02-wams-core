package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateFileService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.common.MmsDetailsDialog;
import eu.isygoit.ui.mms.views.common.MmsDialogSupport;
import eu.isygoit.ui.mms.views.common.MmsEnumTag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

/**
 * Read-only details dialog for a {@link MsgTemplateDto}: Identity, Configuration,
 * File and Audit tabs. {@code id} is never shown and {@code file} (upload payload)
 * is not a displayable field.
 */
@Slf4j
public class MsgTemplateDetailsViewDialog extends MmsDetailsDialog {

    private final MsgTemplateFileService templateFileService;
    private final SenderConfigService senderConfigService;
    private final MsgTemplateDto template;
    private String senderConfigDisplayName;
    private String senderConfigTooltip;

    public MsgTemplateDetailsViewDialog(MsgTemplateFileService templateFileService,
                                        SenderConfigService senderConfigService,
                                        MsgTemplateDto template) {
        super(I18n.t("mms.msgtemplate.dialog.view.title",
                template.getName() != null
                        ? MmsEnumTag.label(template.getName(), "mms.msgtemplate.enum.name")
                        : template.getId()));
        this.templateFileService = templateFileService;
        this.senderConfigService = senderConfigService;
        this.template = template;

        applyWidth(DialogLayout.WIDTH_M);
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
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("mms.msgtemplate.dialog.view.code"),
                template.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("mms.msgtemplate.dialog.view.tenant"),
                template.getTenant());
        identityGrid.add(MmsEnumTag.detailField(VaadinIcon.FILE_TEXT,
                I18n.t("mms.msgtemplate.dialog.view.name"),
                MmsEnumTag.ofValue(template.getName(), "mms.msgtemplate.enum.name")));
        addFieldToGrid(identityGrid, VaadinIcon.INFO_CIRCLE, I18n.t("mms.msgtemplate.dialog.view.description"),
                dash(template.getDescription()));
        addTab(I18n.t("mms.msgtemplate.dialog.view.section.identity"),
                createSection(I18n.t("mms.msgtemplate.dialog.view.section.identity"), identityGrid));

        // ── Configuration — language/default sender ──────────────────────────
        Div configGrid = createDetailGrid();
        configGrid.add(MmsEnumTag.detailField(VaadinIcon.FLAG, I18n.t("mms.msgtemplate.dialog.view.language"),
                MmsEnumTag.ofOrUnknown(template.getLanguage(), "mms.msgtemplate.view.language")));
        addFieldToGrid(configGrid, VaadinIcon.ENVELOPE_O, I18n.t("mms.msgtemplate.dialog.view.defaultSender"),
                template.getDefaultSender());
        String senderConfigValue = senderConfigTooltip != null
                ? senderConfigDisplayName + " – " + senderConfigTooltip
                : senderConfigDisplayName;
        addFieldToGrid(configGrid, VaadinIcon.ENVELOPE, I18n.t("mms.msgtemplate.dialog.view.senderConfig"),
                senderConfigValue, true);
        addTab(I18n.t("mms.msgtemplate.dialog.view.section.configuration"),
                createSection(I18n.t("mms.msgtemplate.dialog.view.section.configuration"), configGrid));

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
        addTab(I18n.t("mms.msgtemplate.dialog.view.section.file"),
                createSection(I18n.t("mms.msgtemplate.dialog.view.section.file"), fileSection));

        addAuditTab(template.getCreatedBy(),
                template.getCreateDate() != null ? DateHelper.formatToHumanReadable(template.getCreateDate()) : null,
                template.getUpdatedBy(),
                template.getUpdateDate() != null ? DateHelper.formatToHumanReadable(template.getUpdateDate()) : null);
    }

    private void downloadTemplate() {
        MmsDialogSupport.downloadTemplateFile(this, templateFileService, template);
    }
}
