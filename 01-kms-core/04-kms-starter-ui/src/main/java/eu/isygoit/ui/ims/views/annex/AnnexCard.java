package eu.isygoit.ui.ims.views.annex;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.annex.dialog.AnnexDetailsDialog;
import eu.isygoit.ui.ims.views.annex.dialog.DeleteAnnexDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AnnexCard extends BaseCard<AnnexManagementView, AnnexService> {

    private final AnnexDto annex;
    private final Runnable onRefresh;

    public AnnexCard(AnnexManagementView parentView,
                     AnnexService annexService,
                     AnnexDto annex,
                     Runnable onRefresh) {
        super(parentView, annexService);
        this.annex = annex;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "annex-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("card-title-group");

        Span titleSpan = buildTitleSpan(annex.getTableCode(), annex.getValue());
        Span langChip = buildStatusChip(
                annex.getLanguage() != null ? annex.getLanguage().name() : I18n.t("ims.annex.card.status.unknown"),
                annex.getLanguage() != null ? annex.getLanguage().name() : I18n.t("ims.annex.card.status.unknown")
        );

        titleLayout.add(titleSpan, langChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("ims.annex.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new AnnexDetailsDialog(parentView, objectService, annex.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("ims.annex.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateAnnexDialog(annex, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("ims.annex.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteAnnexDialog(parentView, objectService, annex.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("card-row--spaced");

        body.add(createIconRow(VaadinIcon.FONT, I18n.t("ims.annex.card.value"), annex.getValue()));
        add(body);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.addClassName("meta-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.addClassName("meta-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }
}