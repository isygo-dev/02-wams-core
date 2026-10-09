package eu.isygoit.ui.mms.views.sender.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.SenderConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.mms.views.common.MmsDetailsDialog;
import lombok.extern.slf4j.Slf4j;

/**
 * Read-only details dialog for a {@link SenderConfigDto}: Identity, Connection,
 * Credentials and Audit tabs. {@code id} is never shown and the raw password is
 * never rendered, only whether one is configured.
 */
@Slf4j
public class SenderConfigDetailsViewDialog extends MmsDetailsDialog {

    private final SenderConfigDto config;

    public SenderConfigDetailsViewDialog(SenderConfigDto config) {
        super(I18n.t("mms.sender.dialog.view.title",
                config.getName() != null ? config.getName() : config.getId()));
        this.config = config;

        applyWidth(DialogLayout.WIDTH_M);
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("sender-config-details-dialog");

        buildContent();
    }

    private void buildContent() {
        // ── Identity — name/code/tenant/description ───────────────────────
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("mms.sender.dialog.view.code"),
                config.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("mms.sender.dialog.view.tenant"),
                config.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.FILE_TEXT, I18n.t("mms.sender.dialog.view.name"),
                config.getName());
        addFieldToGrid(identityGrid, VaadinIcon.INFO_CIRCLE, I18n.t("mms.sender.dialog.view.description"),
                config.getDescription());
        addTab(I18n.t("mms.sender.dialog.view.section.identity"),
                createSection(I18n.t("mms.sender.dialog.view.section.identity"), identityGrid));

        // ── Connection — SMTP host/port/protocol/TLS/debug ────────────────
        Div connectionGrid = createDetailGrid();
        addFieldToGrid(connectionGrid, VaadinIcon.SERVER, I18n.t("mms.sender.dialog.view.host"),
                config.getHost(), true);
        addFieldToGrid(connectionGrid, VaadinIcon.PLUG, I18n.t("mms.sender.dialog.view.port"),
                config.getPort());
        addFieldToGrid(connectionGrid, VaadinIcon.CONNECT, I18n.t("mms.sender.dialog.view.protocol"),
                config.getTransportProtocol() != null ? config.getTransportProtocol() : "smtp");
        addFieldToGrid(connectionGrid, VaadinIcon.LOCK, I18n.t("mms.sender.dialog.view.smtp.auth"),
                config.getSmtpAuth() != null ? config.getSmtpAuth() : "true");

        boolean tlsEnabled = Boolean.TRUE.equals(config.getSmtpStarttlsEnable());
        addFieldToGrid(connectionGrid, VaadinIcon.SHIELD, I18n.t("mms.sender.dialog.view.tls.enable"),
                tlsEnabled ? I18n.t("mms.sender.dialog.view.tls.enabled") : I18n.t("mms.sender.dialog.view.tls.disabled"));

        boolean tlsRequired = Boolean.TRUE.equals(config.getSmtpStarttlsRequired());
        addFieldToGrid(connectionGrid, VaadinIcon.SHIELD, I18n.t("mms.sender.dialog.view.tls.required"),
                tlsRequired ? I18n.t("mms.sender.dialog.view.tls.required.yes") : I18n.t("mms.sender.dialog.view.tls.required.no"));

        boolean debugEnabled = Boolean.TRUE.equals(config.getDebug());
        addFieldToGrid(connectionGrid, VaadinIcon.BUG, I18n.t("mms.sender.dialog.view.debug"),
                debugEnabled ? I18n.t("mms.sender.dialog.view.debug.on") : I18n.t("mms.sender.dialog.view.debug.off"));

        addFieldToGrid(connectionGrid, VaadinIcon.ENVELOPE_O, I18n.t("mms.sender.dialog.view.defaultSender"),
                config.getDefaultSender());
        addTab(I18n.t("mms.sender.dialog.view.section.connection"),
                createSection(I18n.t("mms.sender.dialog.view.section.connection"), connectionGrid));

        // ── Credentials — username only; the raw password is never
        //    rendered, only whether one is configured ───────────────────
        Div credentialsGrid = createDetailGrid();
        addFieldToGrid(credentialsGrid, VaadinIcon.USER, I18n.t("mms.sender.dialog.view.username"),
                config.getUsername(), true);

        boolean hasPassword = config.getPassword() != null && !config.getPassword().isEmpty();
        addFieldToGrid(credentialsGrid, VaadinIcon.KEY, I18n.t("mms.sender.dialog.view.password"),
                hasPassword ? I18n.t("mms.sender.dialog.view.password.set") : I18n.t("mms.sender.dialog.view.password.notSet"));
        addTab(I18n.t("mms.sender.dialog.view.section.credentials"),
                createSection(I18n.t("mms.sender.dialog.view.section.credentials"), credentialsGrid));

        addAuditTab(config.getCreatedBy(),
                config.getCreateDate() != null ? DateHelper.formatToHumanReadable(config.getCreateDate()) : null,
                config.getUpdatedBy(),
                config.getUpdateDate() != null ? DateHelper.formatToHumanReadable(config.getUpdateDate()) : null);
    }
}
