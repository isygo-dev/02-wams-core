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
import eu.isygoit.ui.views.alias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.views.alias.dialog.UpdateAliasDialog;

// -------------------------------------------------------------------------
// Alias Card - Fully Responsive
// -------------------------------------------------------------------------
class AliasCard extends VerticalLayout {
    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final String createDate;

    private HorizontalLayout headerRow;
    private HorizontalLayout buttonBar;
    private Span aliasSpan;

    public AliasCard(AliasesView aliasesView,
                     KmsApiService kmsApiService,
                     KmsDtos.ListAliasesResponse.AliasEntry entry) {
        this.parentView = aliasesView;
        this.kmsApiService = kmsApiService;
        this.aliasName = entry.getAliasName();
        this.targetKeyId = entry.getTargetKeyId();
        this.aliasWrn = entry.getAliasWrn();
        this.createDate = entry.getCreateDate();
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
        addClassName("hover:shadow-m");

        // Header row: alias name + button bar
        headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.getStyle().set("flex-wrap", "wrap");
        headerRow.setSpacing(true);

        aliasSpan = new Span(aliasName);
        aliasSpan.addClassName(LumoUtility.FontWeight.BOLD);
        aliasSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        aliasSpan.addClassName(LumoUtility.TextColor.PRIMARY);
        aliasSpan.getStyle().set("word-break", "break-word");

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Reassign alias");
        updateBtn.addClickListener(e -> updateAlias());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete alias");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> deleteAlias());

        buttonBar.add(updateBtn, deleteBtn);
        headerRow.add(aliasSpan, buttonBar);
        add(headerRow);

        // Target key info
        Span targetSpan = new Span("Target key: " + targetKeyId);
        targetSpan.addClassName(LumoUtility.FontSize.SMALL);
        targetSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        targetSpan.getStyle().set("word-break", "break-word");
        add(targetSpan);

        // Creation date (if present)
        if (createDate != null && !createDate.isEmpty()) {
            Span dateSpan = new Span("Created: " + createDate);
            dateSpan.addClassName(LumoUtility.FontSize.XSMALL);
            dateSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            add(dateSpan);
        }

        // Inject responsive CSS using JavaScript (fixes URL encoding issues)
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
        // Add class names for CSS targeting
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
        new DeleteAliasDialog(parentView, kmsApiService, parentView::loadAliases, aliasName).open();
    }

    private void updateAlias() {
        new UpdateAliasDialog(parentView, kmsApiService, parentView::loadAliases, aliasName, targetKeyId).open();
    }
}