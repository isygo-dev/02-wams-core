package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

/**
 * Read-only dialog showing every field of a {@link TokenConfigDto}, for use
 * when the compact {@code TokenConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class TokenConfigDetailsViewDialog extends DetailsViewDialog {

    public TokenConfigDetailsViewDialog(TokenConfigDto dto) {
        super(I18n.t("kms.token.details.title"));

        setWidth("650px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("token-config-details-dialog");

        buildContent(dto);
    }

    private void buildContent(TokenConfigDto dto) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere — worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.token.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.token.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.token.details.section.identity"), identityGrid));

        // Algorithm / cryptographic + token parameters — how the token is shaped and signed.
        Div algorithmGrid = createDetailGrid();
        addFieldToGrid(algorithmGrid, VaadinIcon.KEY, I18n.t("kms.token.details.field.type"),
                dto.getTokenType() != null ? dto.getTokenType().meaning() : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.CLOCK, I18n.t("kms.token.config.lifetime"),
                dto.getLifeTimeInMs() != null ? formatLifetime(dto.getLifeTimeInMs()) : null);
        // Signature algorithm identifier — copyable for precise reuse in configs even if short.
        addFieldToGrid(algorithmGrid, VaadinIcon.CODE, I18n.t("kms.token.config.algorithm"), dto.getSignatureAlgorithm(), true);
        mainLayout.add(createSection(I18n.t("kms.token.details.section.algorithm"), algorithmGrid));

        // Claims — issuer/audience values embedded into the JWT payload.
        Div claimsGrid = createDetailGrid();
        addFieldToGrid(claimsGrid, VaadinIcon.BUILDING, I18n.t("kms.token.config.issuer"), dto.getIssuer());
        addFieldToGrid(claimsGrid, VaadinIcon.GROUP, I18n.t("kms.token.config.audience"),
                dto.getAudience() != null && !dto.getAudience().isEmpty() ? String.join(", ", dto.getAudience()) : null);
        mainLayout.add(createSection(I18n.t("kms.token.details.section.claims"), claimsGrid));

        Div keyGrid = createDetailGrid();
        // KMS key id is a reference identifier — copyable even if short.
        addFieldToGrid(keyGrid, VaadinIcon.KEY_O, I18n.t("kms.token.details.field.kms.key.id"), dto.getKmsKeyId(), true);
        // Secret key is displayed masked; no copy button since the raw secret isn't shown.
        addFieldToGrid(keyGrid, VaadinIcon.LOCK, I18n.t("kms.token.details.field.secret.key"), maskSecret(dto.getSecretKey()), false);
        // Public key is meant to be shared/copied for verification elsewhere.
        addFieldToGrid(keyGrid, VaadinIcon.UNLOCK, I18n.t("kms.token.details.field.public.key"), dto.getPublicKey(), true);
        mainLayout.add(createSection(I18n.t("kms.token.details.section.key"), keyGrid));

        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.token.details.field.created"), dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.token.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.token.details.field.updated"), dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.token.details.field.updated.by"), dto.getUpdatedBy());
        mainLayout.add(createSection(I18n.t("kms.token.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) return null;
        return "•".repeat(Math.min(12, Math.max(6, secret.length())));
    }

    private String formatLifetime(Integer lifeTimeInMs) {
        if (lifeTimeInMs == null || lifeTimeInMs <= 0) return null;
        long seconds = lifeTimeInMs / 1000;
        if (seconds < 60) return seconds + " " + I18n.t("time.seconds");
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " " + I18n.t("time.minutes");
        long hours = minutes / 60;
        if (hours < 24) return hours + " " + I18n.t("time.hours");
        long days = hours / 24;
        return days + " " + I18n.t("time.days");
    }
}
