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
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog.UpdateAliasDialog;

import java.util.List;

class AliasCard extends BaseCard<AliasesView, KmsApiService> {

    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final Boolean primaryKey;
    private final String createDate;

    AliasCard(AliasesView aliasesView,
              KmsApiService kmsApiService,
              KmsDtos.ListAliasesResponse.AliasEntry entry) {
        super(aliasesView, kmsApiService);
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
        left.add(KmsMainView.createCopyButton(VaadinIcon.COPY, aliasName, "Copy alias name"));

        if (Boolean.TRUE.equals(primaryKey)) {
            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setColor("var(--lumo-error-color)");
            warningIcon.setSize("18px");
            warningIcon.setTooltipText("Primary key alias – handle with care.");
            left.add(warningIcon);

            Span primaryBadge = new Span("PRIMARY");
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
        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Update alias");
        updateBtn.addClickListener(e -> updateAlias());

        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, "Delete alias");
        deleteBtn.addClickListener(e -> deleteAlias());

        return List.of(updateBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRowWithCopy(VaadinIcon.KEY, "Target key", targetKeyId, targetKeyId));
        if (aliasWrn != null && !aliasWrn.isBlank()) {
            add(createIconRowWithCopy(VaadinIcon.TAG, "WRN", aliasWrn, aliasWrn));
        }
        if (createDate != null && !createDate.isBlank()) {
            add(createIconRow(VaadinIcon.CALENDAR, "Created", createDate));
        }
        if (Boolean.TRUE.equals(primaryKey)) {
            add(createIconRow(VaadinIcon.EXCLAMATION_CIRCLE, "Note", "This is the primary key alias. Handle with care."));
        }
    }

    // ── Helper row builders (identical to previous version) ──────────────────
    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createIconRowWithCopy(VaadinIcon icon, String label, String value, String copyValue) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        Button copyBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, copyValue, "Copy");
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        row.add(iconComponent, labelSpan, valueSpan, copyBtn);
        row.expand(valueSpan);
        return row;
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    private void deleteAlias() {
        new DeleteAliasDialog(parentView, objectService, parentView::resetPaginationAndLoad,
                aliasName, primaryKey).open();
    }

    private void updateAlias() {
        new UpdateAliasDialog(parentView, objectService, parentView::resetPaginationAndLoad,
                aliasName, targetKeyId).open();
    }

    // ── Extra CSS (responsive) ────────────────────────────────────────────────
    @Override
    protected String buildExtraStyles() {
        return """
                .alias-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .alias-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .alias-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .alias-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}