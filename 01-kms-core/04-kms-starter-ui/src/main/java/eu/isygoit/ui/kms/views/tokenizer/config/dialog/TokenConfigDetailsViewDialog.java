package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only view of a {@link TokenConfigDto}: every DTO field except
 * {@code id} (never displayed) is shown; audit fields live in the audit tab.
 * The secret key is only shown masked.
 */
@CssImport("./styles/kms.scss")
public class TokenConfigDetailsViewDialog extends KmsDetailsDialog {

    private final TokenConfigDto dto;

    public TokenConfigDetailsViewDialog(TokenConfigDto dto) {
        super(I18n.t("kms.token.details.title"));
        this.dto = dto;
        applyWidth(DialogLayout.WIDTH_L);
        buildContent();
    }

    private void buildContent() {
        Div identityGrid = createDetailGrid();
        // Config code is the identifier used to reference this config elsewhere - worth copying.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.token.details.field.code"), dto.getCode(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.token.details.field.tenant"), dto.getTenant());
        addTab(I18n.t("kms.token.details.section.identity"),
                createSection(I18n.t("kms.token.details.section.identity"), identityGrid));

        // Algorithm / cryptographic + token parameters: how the token is shaped and signed.
        Div algorithmGrid = createDetailGrid();
        algorithmGrid.add(KmsEnumTag.detailField(
                VaadinIcon.KEY, I18n.t("kms.token.details.field.type"),
                KmsEnumTag.ofOrUnknown(dto.getTokenType(), "kms.enum")));
        addFieldToGrid(algorithmGrid, VaadinIcon.CLOCK, I18n.t("kms.token.config.lifetime"),
                dto.getLifeTimeInMs() != null ? formatLifetime(dto.getLifeTimeInMs()) : null);
        // Signature algorithm identifier - copyable for precise reuse in configs even if short.
        addFieldToGrid(algorithmGrid, VaadinIcon.CODE, I18n.t("kms.token.config.algorithm"), dto.getSignatureAlgorithm(), true);
        addTab(I18n.t("kms.token.details.section.algorithm"),
                createSection(I18n.t("kms.token.details.section.algorithm"), algorithmGrid));

        // Claims: issuer/audience values embedded into the JWT payload.
        Div claimsGrid = createDetailGrid();
        addFieldToGrid(claimsGrid, VaadinIcon.BUILDING, I18n.t("kms.token.config.issuer"), dto.getIssuer());
        addFieldToGrid(claimsGrid, VaadinIcon.GROUP, I18n.t("kms.token.config.audience"),
                dto.getAudience() != null && !dto.getAudience().isEmpty() ? String.join(", ", dto.getAudience()) : null);
        addTab(I18n.t("kms.token.details.section.claims"),
                createSection(I18n.t("kms.token.details.section.claims"), claimsGrid));

        Div keyGrid = createDetailGrid();
        // KMS key id is a reference identifier - copyable even if short.
        addFieldToGrid(keyGrid, VaadinIcon.KEY_O, I18n.t("kms.token.details.field.kms.key.id"), dto.getKmsKeyId(), true);
        // Secret key is displayed masked; no copy button since the raw secret isn't shown.
        addFieldToGrid(keyGrid, VaadinIcon.LOCK, I18n.t("kms.token.details.field.secret.key"), maskSecret(dto.getSecretKey()), false);
        // Public key is meant to be shared/copied for verification elsewhere.
        addFieldToGrid(keyGrid, VaadinIcon.UNLOCK, I18n.t("kms.token.details.field.public.key"), dto.getPublicKey(), true);
        addTab(I18n.t("kms.token.details.section.key"),
                createSection(I18n.t("kms.token.details.section.key"), keyGrid));

        addAuditTab(dto.getCreatedBy(), dto.getCreateDate(), dto.getUpdatedBy(), dto.getUpdateDate());
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
