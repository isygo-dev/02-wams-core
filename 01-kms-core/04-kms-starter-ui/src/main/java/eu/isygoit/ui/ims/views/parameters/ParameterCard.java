package eu.isygoit.ui.ims.views.parameters;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.parameters.dialog.DeleteParameterDialog;
import eu.isygoit.ui.ims.views.parameters.dialog.ParameterDetailsDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ParameterCard extends BaseCard<ParameterManagementView, AppParameterService> {

    private final AppParameterDto parameter;
    private final Runnable onRefresh;

    public ParameterCard(ParameterManagementView parentView,
                         AppParameterService parameterService,
                         AppParameterDto parameter,
                         Runnable onRefresh) {
        super(parentView, parameterService);
        this.parameter = parameter;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected Component buildTitle() {
        return new Div(); // not used
    }

    @Override
    protected String cardCssClassName() {
        return "parameter-card";
    }

    @Override
    protected void buildHeader() {
        // Row 1: name + value summary
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(parameter.getName(), parameter.getValue());
        row1.add(titleSpan);

        // Action buttons
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        row1.add(buttonBar);
        row1.expand(buttonBar);

        add(row1);
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full parameter details");
        detailsBtn.addClickListener(e -> new ParameterDetailsDialog(parentView, objectService, parameter.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit parameter");
        editBtn.addClickListener(e -> parentView.openUpdateParameterDialog(parameter, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete parameter");
        deleteBtn.addClickListener(e -> new DeleteParameterDialog(parentView, objectService, parameter.getId(), () -> {
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

        body.add(createIconRow(VaadinIcon.KEY, "Name", parameter.getName()));
        body.add(createIconRow(VaadinIcon.INPUT, "Value", parameter.getValue()));
        if (parameter.getTenant() != null && !parameter.getTenant().isBlank()) {
            body.add(createIconRow(VaadinIcon.BUILDING, "Tenant", parameter.getTenant()));
        }
        if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, "Description", parameter.getDescription()));
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
                .parameter-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .parameter-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .parameter-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .parameter-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .parameter-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .parameter-card .parameter-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}