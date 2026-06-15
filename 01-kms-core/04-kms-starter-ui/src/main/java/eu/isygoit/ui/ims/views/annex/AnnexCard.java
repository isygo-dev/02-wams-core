package eu.isygoit.ui.ims.views.annex;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.annex.dialog.DeleteAnnexDialog;
import eu.isygoit.ui.ims.views.annex.dialog.AnnexDetailsDialog;
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
    protected Component buildTitle() {
        return new Div(); // not used
    }

    @Override
    protected String cardCssClassName() {
        return "annex-card";
    }

    @Override
    protected void buildHeader() {
        // Row 1: title + chip (language)
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(annex.getTableCode(), annex.getValue());
        Span langChip = buildStatusChip(
                annex.getLanguage() != null ? annex.getLanguage().name() : "UNKNOWN",
                annex.getLanguage() != null ? annex.getLanguage().name() : "UNKNOWN"
        );
        row1.add(titleSpan, langChip);

        // Action buttons
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        // Add both rows
        add(row1, buttonBar);
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full annex details");
        detailsBtn.addClickListener(e -> new AnnexDetailsDialog(parentView, objectService, annex.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit annex");
        editBtn.addClickListener(e -> parentView.openUpdateAnnexDialog(annex, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete annex");
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
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.CODE, "Table code", annex.getTableCode()));
        body.add(createIconRow(VaadinIcon.FONT, "Value", annex.getValue()));
        if (annex.getDescription() != null && !annex.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, "Description", annex.getDescription()));
        }
        if (annex.getReference() != null && !annex.getReference().isBlank()) {
            body.add(createIconRow(VaadinIcon.LINK, "Reference", annex.getReference()));
        }
        if (annex.getAnnexOrder() != null) {
            body.add(createIconRow(VaadinIcon.SORT, "Order", String.valueOf(annex.getAnnexOrder())));
        }
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
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .annex-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .annex-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .annex-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .annex-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .annex-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .annex-card .annex-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}