package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class ParameterDetailsDialog extends NoActionDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final Long parameterId;

    public ParameterDetailsDialog(ParameterManagementView parentView,
                                  AppParameterService parameterService,
                                  Long parameterId) {
        super("Parameter Details");
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameterId = parameterId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("parameter-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AppParameterDto> response = parameterService.findById(parameterId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span("Parameter not found"));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span("Failed to load details: " + extractErrorMessage(ex)));
            addCloseButton();
        } catch (Exception e) {
            add(new Span("Error: " + e.getMessage()));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AppParameterDto param) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div infoGrid = new Div();
        infoGrid.addClassName("details-grid");
        infoGrid.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(infoGrid, VaadinIcon.KEY, "Name", param.getName());
        addFieldToGrid(infoGrid, VaadinIcon.INPUT, "Value", param.getValue());
        addFieldToGrid(infoGrid, VaadinIcon.BUILDING, "Tenant", param.getTenant());
        addFieldToGrid(infoGrid, VaadinIcon.FILE_TEXT, "Description", param.getDescription());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, "Created", param.getCreateDate() != null ? DateHelper.formatToHumanReadable(param.getCreateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.USER_CHECK, "Created by", param.getCreatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, "Updated", param.getUpdateDate() != null ? DateHelper.formatToHumanReadable(param.getUpdateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.EDIT, "Updated by", param.getUpdatedBy());

        mainLayout.add(createSection("Parameter Information", infoGrid));

        add(mainLayout);
        addCloseButton();
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-primary-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
                .set("margin-bottom", "var(--lumo-space-s)")
                .set("padding-bottom", "var(--lumo-space-xs)");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button("Close", e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}