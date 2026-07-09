package eu.isygoit.ui.kms.views.cryptography.keyAlias;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.AliasDetailsViewDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.UpdateAliasDialog;

import java.util.List;

class AliasCard extends BaseCard<AliasesView, KmsApiService> {

    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final Boolean primaryKey;
    private final String createDate;
    private final KmsDtos.ListAliasesResponse.AliasEntry entry;

    AliasCard(AliasesView aliasesView,
              KmsApiService kmsApiService,
              KmsDtos.ListAliasesResponse.AliasEntry entry) {
        super(aliasesView, kmsApiService);
        this.entry = entry;
        this.aliasName = entry.getAliasName();
        this.targetKeyId = entry.getTargetKeyId();
        this.aliasWrn = entry.getAliasWrn();
        this.createDate = entry.getCreateDate();
        this.primaryKey = entry.getPrimaryKey();
        initCard();
    }

    // ── Public accessors (used by parent view) ───────────────────────────────
    public String getAliasName() {
        return aliasName;
    }

    public String getTargetKeyId() {
        return targetKeyId;
    }

    // ── BaseCard implementation ───────────────────────────────────────────────
    @Override
    protected String cardCssClassName() {
        return "alias-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        Span aliasSpan = buildTitleSpan(aliasName, aliasName);
        left.add(aliasSpan);
        left.add(KmsMainView.createCopyButton(VaadinIcon.COPY, aliasName, I18n.t("kms.alias.card.copy.alias.tooltip")));

        if (Boolean.TRUE.equals(primaryKey)) {
            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setColor("var(--lumo-error-color)");
            warningIcon.setSize("18px");
            warningIcon.setTooltipText(I18n.t("kms.alias.card.primary.tooltip"));
            left.add(warningIcon);

            Span primaryBadge = new Span(I18n.t("kms.alias.card.primary.badge"));
            primaryBadge.getElement().getThemeList().add("badge");
            primaryBadge.addClassName(LumoUtility.Background.ERROR_10);
            primaryBadge.addClassName(LumoUtility.TextColor.ERROR);
            primaryBadge.addClassName(LumoUtility.FontSize.XSMALL);
            left.add(primaryBadge);
        }
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        // Standard order: Details → Edit → Delete (Delete always last, always danger-styled).
        Button detailsBtn = createDetailsButton(I18n.t("kms.alias.card.details.tooltip"), this::showDetails);
        Button updateBtn = createEditButton(I18n.t("kms.alias.card.update.tooltip"), this::updateAlias);
        Button deleteBtn = createDeleteButton(I18n.t("kms.alias.card.delete.tooltip"), this::deleteAlias);

        return List.of(detailsBtn, updateBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRowWithCopy(VaadinIcon.KEY, I18n.t("kms.alias.card.target.key"), targetKeyId, targetKeyId));
        if (aliasWrn != null && !aliasWrn.isBlank()) {
            add(createIconRowWithCopy(VaadinIcon.TAG, I18n.t("kms.alias.card.wrn"), aliasWrn, aliasWrn));
        }
        if (createDate != null && !createDate.isBlank()) {
            add(createIconRow(VaadinIcon.CALENDAR, I18n.t("kms.alias.card.created"), createDate));
        }
        if (Boolean.TRUE.equals(primaryKey)) {
            add(createIconRow(VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.alias.card.primary.note"), ""));
        }
    }

    // ── Helper row builders ──────────────────────────────────────────────────
    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("alias-card__row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("alias-card__row-label");

        Span valueSpan = new Span(value != null ? value : I18n.t("kms.alias.card.masked"));
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("alias-card__row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createIconRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("alias-card__row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("alias-card__row-label");

        Span valueSpan = new Span(value != null ? value : I18n.t("kms.alias.card.masked"));
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("alias-card__row-value");

        Button copyBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, copyValue, I18n.t("kms.alias.card.copy.tooltip"));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        row.add(iconComponent, labelSpan, valueSpan, copyBtn);
        row.expand(valueSpan);
        return row;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void showDetails() {
        new AliasDetailsViewDialog(entry).open();
    }

    private void deleteAlias() {
        new DeleteAliasDialog(parentView, objectService, parentView::resetPaginationAndLoad,
                aliasName, primaryKey).open();
    }

    private void updateAlias() {
        new UpdateAliasDialog(parentView, objectService, parentView::resetPaginationAndLoad,
                aliasName, targetKeyId).open();
    }
}