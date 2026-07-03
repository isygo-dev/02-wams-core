package eu.isygoit.ui.ims.views.parameters;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.i18n.I18n;
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
    protected String cardCssClassName() {
        return "parameter-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(parameter.getName(), parameter.getValue());
        titleLayout.add(titleSpan);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("ims.parameter.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new ParameterDetailsDialog(parentView, objectService, parameter.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("ims.parameter.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateParameterDialog(parameter, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("ims.parameter.card.delete.tooltip"));
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

        body.add(createIconRow(VaadinIcon.KEY, I18n.t("ims.parameter.card.name"), parameter.getName()));
        body.add(createIconRow(VaadinIcon.INPUT, I18n.t("ims.parameter.card.value"), parameter.getValue()));
        if (parameter.getTenant() != null && !parameter.getTenant().isBlank()) {
            body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("ims.parameter.card.tenant"), parameter.getTenant()));
        }
        if (parameter.getDescription() != null && !parameter.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("ims.parameter.card.description"), parameter.getDescription()));
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