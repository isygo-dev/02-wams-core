package eu.isygoit.ui.views.alias;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.alias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.views.alias.dialog.UpdateAliasDialog;

class AliasCard extends VerticalLayout {
    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final Boolean primaryKey;
    private final String createDate;

    private HorizontalLayout headerRow;
    private HorizontalLayout buttonBar;
    private Span aliasSpan;
    private Span targetSpan;
    private Span dateSpan;

    public AliasCard(AliasesView aliasesView,
                     KmsApiService kmsApiService,
                     KmsDtos.ListAliasesResponse.AliasEntry entry) {
        this.parentView = aliasesView;
        this.kmsApiService = kmsApiService;
        this.aliasName = entry.getAliasName();
        this.targetKeyId = entry.getTargetKeyId();
        this.aliasWrn = entry.getAliasWrn();
        this.createDate = entry.getCreateDate();
        this.primaryKey = entry.getPrimaryKey();
        buildCard();
        addClassName("alias-card");
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getTargetKeyId() {
        return targetKeyId;
    }

    private void buildCard() {
        setWidthFull();
        setMargin(false);
        setPadding(true);
        addClassName(LumoUtility.BorderRadius.LARGE);
        addClassName(LumoUtility.Background.BASE);
        addClassName(LumoUtility.BoxShadow.XSMALL);
        getStyle().set("transition", "all 0.2s ease-in-out");

        // Left side: alias name + copy + primary badge
        HorizontalLayout leftSide = new HorizontalLayout();
        leftSide.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSide.setSpacing(true);

        aliasSpan = new Span(aliasName);
        aliasSpan.addClassName(LumoUtility.FontWeight.BOLD);
        aliasSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        aliasSpan.addClassName(LumoUtility.TextColor.PRIMARY);
        aliasSpan.getStyle().set("word-break", "break-word");
        aliasSpan.getElement().setAttribute("title", aliasName);
        leftSide.add(aliasSpan);

        // Copy alias name button (compact)
        leftSide.add(MainView.createCopyButton(VaadinIcon.COPY_O, aliasName, "Copy alias name"));

        // Primary key warning and badge (no copy button for warning text)
        if (Boolean.TRUE.equals(primaryKey)) {
            Icon warningIcon = VaadinIcon.WARNING.create();
            warningIcon.setColor("var(--lumo-error-color)");
            warningIcon.setSize("18px");
            warningIcon.setTooltipText("Primary key alias – this alias points to the default master key. " +
                    "Deleting or reassigning it may affect key operations.");
            leftSide.add(warningIcon);

            Span primaryBadge = new Span("PRIMARY");
            primaryBadge.getElement().getThemeList().add("badge");
            primaryBadge.addClassName(LumoUtility.Background.ERROR_10);
            primaryBadge.addClassName(LumoUtility.TextColor.ERROR);
            primaryBadge.addClassName(LumoUtility.FontSize.XSMALL);
            primaryBadge.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            primaryBadge.addClassName(LumoUtility.BorderRadius.MEDIUM);
            leftSide.add(primaryBadge);
        }

        // Right side buttons
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Update alias (rename or reassign)");
        updateBtn.addClickListener(e -> updateAlias());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete this alias");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> deleteAlias());

        buttonBar.add(updateBtn, deleteBtn);

        headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");
        headerRow.setSpacing(true);
        headerRow.add(leftSide, buttonBar);
        add(headerRow);

        // Target key row with copy button
        HorizontalLayout targetRow = new HorizontalLayout();
        targetRow.setAlignItems(FlexComponent.Alignment.CENTER);
        targetRow.setSpacing(true);
        targetSpan = new Span("Target key: " + targetKeyId);
        targetSpan.addClassName(LumoUtility.FontSize.SMALL);
        targetSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        targetSpan.getStyle().set("word-break", "break-word");
        targetSpan.getElement().setAttribute("title", targetKeyId);
        targetRow.add(targetSpan);
        targetRow.add(MainView.createCopyButton(VaadinIcon.COPY_O, targetKeyId, "Copy target key ID"));
        add(targetRow);

        // Alias WRN row with copy button (if present)
        if (aliasWrn != null && !aliasWrn.isBlank()) {
            HorizontalLayout wrnRow = new HorizontalLayout();
            wrnRow.setAlignItems(FlexComponent.Alignment.CENTER);
            wrnRow.setSpacing(true);
            Span wrnSpan = new Span("WRN: " + aliasWrn);
            wrnSpan.addClassName(LumoUtility.FontSize.SMALL);
            wrnSpan.addClassName(LumoUtility.TextColor.SECONDARY);
            wrnSpan.getStyle().set("word-break", "break-word");
            wrnSpan.getElement().setAttribute("title", aliasWrn);
            wrnRow.add(wrnSpan);
            wrnRow.add(MainView.createCopyButton(VaadinIcon.COPY_O, aliasWrn, "Copy alias WRN"));
            add(wrnRow);
        }

        // Creation date (if present)
        if (createDate != null && !createDate.isEmpty()) {
            dateSpan = new Span("Created: " + createDate);
            dateSpan.addClassName(LumoUtility.FontSize.XSMALL);
            dateSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            dateSpan.getElement().setAttribute("title", createDate);
            add(dateSpan);
        }

        // Extra warning line for primary key (no copy button, matches KeyCard's extra info)
        if (Boolean.TRUE.equals(primaryKey)) {
            HorizontalLayout warningLine = new HorizontalLayout();
            warningLine.setAlignItems(FlexComponent.Alignment.CENTER);
            warningLine.setSpacing(true);
            Span extraWarning = new Span("⚠️ This is the primary key alias. Handle with care.");
            extraWarning.addClassName(LumoUtility.FontSize.XSMALL);
            extraWarning.addClassName(LumoUtility.TextColor.ERROR);
            extraWarning.getStyle().set("margin-top", "var(--lumo-space-xs)");
            warningLine.add(extraWarning);
            add(warningLine);
        }

        injectResponsiveStyles();
    }

    private void injectResponsiveStyles() {
        String css = """
                    .alias-card .alias-header-row {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-s);
                        align-items: center;
                        justify-content: space-between;
                        width: 100%;
                    }
                    .alias-card .alias-button-bar {
                        display: flex;
                        flex-wrap: wrap;
                        gap: var(--lumo-space-xs);
                    }
                    @media (max-width: 640px) {
                        .alias-card .alias-header-row {
                            flex-direction: column;
                            align-items: flex-start;
                        }
                        .alias-card .alias-button-bar {
                            width: 100%;
                            justify-content: flex-start;
                        }
                        .alias-card .alias-button-bar > * {
                            flex: 1;
                        }
                    }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        headerRow.addClassName("alias-header-row");
        buttonBar.addClassName("alias-button-bar");
    }

    private Button createIconButton(VaadinIcon icon, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        return btn;
    }

    private void deleteAlias() {
        new DeleteAliasDialog(parentView, kmsApiService, parentView::resetPaginationAndLoad, aliasName, primaryKey).open();
    }

    private void updateAlias() {
        new UpdateAliasDialog(parentView, kmsApiService, parentView::resetPaginationAndLoad, aliasName, targetKeyId).open();
    }
}