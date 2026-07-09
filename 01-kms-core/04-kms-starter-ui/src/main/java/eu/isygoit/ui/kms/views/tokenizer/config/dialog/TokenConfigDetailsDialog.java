package eu.isygoit.ui.kms.views.tokenizer.config.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a {@link TokenConfigDto}, for use
 * when the compact {@code TokenConfigCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class TokenConfigDetailsDialog extends NoActionDialog {

    public TokenConfigDetailsDialog(TokenConfigDto dto) {
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

        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.token.details.field.code"), dto.getCode());
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.token.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.token.details.section.identity"), identityGrid));

        // Algorithm / cryptographic + token parameters — how the token is shaped and signed.
        Div algorithmGrid = new Div();
        algorithmGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(algorithmGrid, VaadinIcon.KEY, I18n.t("kms.token.details.field.type"),
                dto.getTokenType() != null ? dto.getTokenType().meaning() : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.CLOCK, I18n.t("kms.token.config.lifetime"),
                dto.getLifeTimeInMs() != null ? formatLifetime(dto.getLifeTimeInMs()) : null);
        addFieldToGrid(algorithmGrid, VaadinIcon.CODE, I18n.t("kms.token.config.algorithm"), dto.getSignatureAlgorithm());
        mainLayout.add(createSection(I18n.t("kms.token.details.section.algorithm"), algorithmGrid));

        // Claims — issuer/audience values embedded into the JWT payload.
        Div claimsGrid = new Div();
        claimsGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(claimsGrid, VaadinIcon.BUILDING, I18n.t("kms.token.config.issuer"), dto.getIssuer());
        addFieldToGrid(claimsGrid, VaadinIcon.GROUP, I18n.t("kms.token.config.audience"),
                dto.getAudience() != null && !dto.getAudience().isEmpty() ? String.join(", ", dto.getAudience()) : null);
        mainLayout.add(createSection(I18n.t("kms.token.details.section.claims"), claimsGrid));

        Div keyGrid = new Div();
        keyGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(keyGrid, VaadinIcon.KEY_O, I18n.t("kms.token.details.field.kms.key.id"), dto.getKmsKeyId());
        addFieldToGrid(keyGrid, VaadinIcon.LOCK, I18n.t("kms.token.details.field.secret.key"), maskSecret(dto.getSecretKey()));
        addFieldToGrid(keyGrid, VaadinIcon.UNLOCK, I18n.t("kms.token.details.field.public.key"), dto.getPublicKey());
        mainLayout.add(createSection(I18n.t("kms.token.details.section.key"), keyGrid));

        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");
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

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;

        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, labelSpan);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        container.add(field);
    }

    private VerticalLayout createSection(String title, Div content) {
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
}
