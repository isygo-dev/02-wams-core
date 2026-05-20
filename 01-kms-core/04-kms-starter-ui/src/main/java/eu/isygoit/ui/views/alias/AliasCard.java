package eu.isygoit.ui.views.alias;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.alias.dialog.DeleteAliasDialog;
import eu.isygoit.ui.views.alias.dialog.UpdateAliasDialog;

// -------------------------------------------------------------------------
// Alias Card
// -------------------------------------------------------------------------
class AliasCard extends VerticalLayout {
    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final String aliasName;
    private final String targetKeyId;
    private final String aliasWrn;
    private final String createDate;

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

        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        Span aliasSpan = new Span(aliasName);
        aliasSpan.addClassName(LumoUtility.FontWeight.BOLD);
        aliasSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        aliasSpan.addClassName(LumoUtility.TextColor.PRIMARY);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete alias");
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> deleteAlias());

        Button updateBtn = createIconButton(VaadinIcon.EDIT, "Reassign alias");
        updateBtn.addClickListener(e -> updateAlias());

        buttonBar.add(updateBtn, deleteBtn);
        headerRow.add(aliasSpan, buttonBar);
        headerRow.expand(aliasSpan);
        add(headerRow);

        Span targetSpan = new Span("Target key: " + targetKeyId);
        targetSpan.addClassName(LumoUtility.FontSize.SMALL);
        targetSpan.addClassName(LumoUtility.TextColor.SECONDARY);
        add(targetSpan);

        if (createDate != null && !createDate.isEmpty()) {
            Span dateSpan = new Span("Created: " + createDate);
            dateSpan.addClassName(LumoUtility.FontSize.XSMALL);
            dateSpan.addClassName(LumoUtility.TextColor.TERTIARY);
            add(dateSpan);
        }
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
