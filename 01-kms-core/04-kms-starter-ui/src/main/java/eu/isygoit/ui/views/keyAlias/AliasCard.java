package eu.isygoit.ui.views.keyAlias;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.AbstractKmsCard;
import eu.isygoit.ui.views.keyAlias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.views.keyAlias.dialog.UpdateAliasDialog;

import java.util.List;

class AliasCard extends AbstractKmsCard<AliasesView> {

    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final Boolean primaryKey;
    private final String createDate;

    // ── Constructor ───────────────────────────────────────────────────────────

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

    // ── Public accessors ──────────────────────────────────────────────────────

    public String getAliasName() {
        return aliasName;
    }

    public String getTargetKeyId() {
        return targetKeyId;
    }

    // ── AbstractKmsCard contract ──────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "alias-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);

        // Alias name + copy
        Span aliasSpan = buildTitleSpan(aliasName, aliasName);
        left.add(aliasSpan);
        left.add(MainView.createCopyButton(VaadinIcon.COPY, aliasName, "Copy alias name"));

        // PRIMARY badge + warning icon
        if (Boolean.TRUE.equals(primaryKey)) {
            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setColor("var(--lumo-error-color)");
            warningIcon.setSize("18px");
            warningIcon.setTooltipText("Primary key alias – this alias points to the default master key. " +
                    "Deleting or reassigning it may affect key operations.");
            left.add(warningIcon);

            Span primaryBadge = new Span("PRIMARY");
            primaryBadge.getElement().getThemeList().add("badge");
            primaryBadge.addClassName(LumoUtility.Background.ERROR_10);
            primaryBadge.addClassName(LumoUtility.TextColor.ERROR);
            primaryBadge.addClassName(LumoUtility.FontSize.XSMALL);
            primaryBadge.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            primaryBadge.addClassName(LumoUtility.BorderRadius.MEDIUM);
            left.add(primaryBadge);
        }

        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Update alias (rename or reassign)");
        updateBtn.addClickListener(e -> updateAlias());

        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, "Delete this alias");
        deleteBtn.addClickListener(e -> deleteAlias());

        return List.of(updateBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        // Target key row with copy button
        HorizontalLayout targetRow = new HorizontalLayout();
        targetRow.setAlignItems(FlexComponent.Alignment.CENTER);
        targetRow.setSpacing(true);

        Span targetSpan = new Span("Target key: " + targetKeyId);
        targetSpan.addClassName(LumoUtility.FontSize.SMALL);
        targetSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        targetSpan.getStyle().set("word-break", "break-word");
        targetSpan.getElement().setAttribute("title", targetKeyId);

        targetRow.add(targetSpan, MainView.createCopyButton(VaadinIcon.COPY, targetKeyId, "Copy target key ID"));
        add(targetRow);

        // WRN row (optional)
        if (aliasWrn != null && !aliasWrn.isBlank()) {
            HorizontalLayout wrnRow = new HorizontalLayout();
            wrnRow.setAlignItems(FlexComponent.Alignment.CENTER);
            wrnRow.setSpacing(true);

            Span wrnSpan = new Span("WRN: " + aliasWrn);
            wrnSpan.addClassName(LumoUtility.FontSize.SMALL);
            wrnSpan.addClassName(LumoUtility.TextColor.SECONDARY);
            wrnSpan.getStyle().set("word-break", "break-word");
            wrnSpan.getElement().setAttribute("title", aliasWrn);

            wrnRow.add(wrnSpan, MainView.createCopyButton(VaadinIcon.COPY, aliasWrn, "Copy alias WRN"));
            add(wrnRow);
        }

        // Creation date (optional)
        if (createDate != null && !createDate.isEmpty()) {
            Span dateSpan = new Span("Created: " + createDate);
            dateSpan.addClassName(LumoUtility.FontSize.XSMALL);
            dateSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            dateSpan.getElement().setAttribute("title", createDate);
            add(dateSpan);
        }

        // Extra warning line for primary key
        if (Boolean.TRUE.equals(primaryKey)) {
            Span extraWarning = new Span("⚠️ This is the primary key alias. Handle with care.");
            extraWarning.addClassName(LumoUtility.FontSize.XSMALL);
            extraWarning.addClassName(LumoUtility.TextColor.ERROR);
            extraWarning.getStyle().set("margin-top", "var(--lumo-space-xs)");
            add(extraWarning);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void deleteAlias() {
        new DeleteAliasDialog(parentView, kmsApiService, parentView::resetPaginationAndLoad,
                aliasName, primaryKey).open();
    }

    private void updateAlias() {
        new UpdateAliasDialog(parentView, kmsApiService, parentView::resetPaginationAndLoad,
                aliasName, targetKeyId).open();
    }
}