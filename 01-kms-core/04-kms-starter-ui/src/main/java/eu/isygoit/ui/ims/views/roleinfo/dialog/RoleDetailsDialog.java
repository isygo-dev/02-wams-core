package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class RoleDetailsDialog extends NoActionDialog {

    private final RoleManagementView parentView;
    private final RoleInfoService roleService;
    private final Long roleId;

    public RoleDetailsDialog(RoleManagementView parentView,
                             RoleInfoService roleService,
                             Long roleId) {
        super("Role Details");
        this.parentView = parentView;
        this.roleService = roleService;
        this.roleId = roleId;

        setWidth("900px");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("role-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<RoleInfoDto> response = roleService.findById(roleId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span("Role not found"));
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

    private void buildContent(RoleInfoDto role) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Basic info grid
        Div basicInfo = new Div();
        basicInfo.getStyle().set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-s)");

        addFieldToGrid(basicInfo, VaadinIcon.USER, "Name", role.getName());
        addFieldToGrid(basicInfo, VaadinIcon.CODE, "Code", role.getCode());
        addFieldToGrid(basicInfo, VaadinIcon.SORT, "Level", String.valueOf(role.getLevel()));
        addFieldToGrid(basicInfo, VaadinIcon.USERS, "Number of users", String.valueOf(role.getNumberOfUsers()));
        addFieldToGrid(basicInfo, VaadinIcon.CALENDAR, "Created", role.getCreateDate() != null ? DateHelper.formatToHumanReadable(role.getCreateDate()) : null);
        addFieldToGrid(basicInfo, VaadinIcon.USER_CHECK, "Created by", role.getCreatedBy());

        mainLayout.add(createSection("General Information", basicInfo));

        if (role.getDescription() != null && !role.getDescription().isBlank()) {
            HorizontalLayout descRow = new HorizontalLayout();
            descRow.setAlignItems(FlexComponent.Alignment.START);
            descRow.setSpacing(true);
            descRow.setWidthFull();
            Icon descIcon = VaadinIcon.FILE_TEXT.create();
            descIcon.setSize("16px");
            descIcon.getStyle().set("color", "var(--lumo-primary-color)");
            Span descLabel = new Span("Description:");
            descLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            Span descValue = new Span(role.getDescription());
            descValue.getStyle().set("flex", "1");
            descRow.add(descIcon, descLabel, descValue);
            descRow.expand(descValue);
            mainLayout.add(descRow);
        }

        // Allowed Applications
        if (role.getAllowedTools() != null && !role.getAllowedTools().isEmpty()) {
            Grid<ApplicationDto> appsGrid = new Grid<>();
            appsGrid.addColumn(ApplicationDto::getName).setHeader("Name");
            appsGrid.addColumn(ApplicationDto::getTitle).setHeader("Title");
            appsGrid.addColumn(ApplicationDto::getCategory).setHeader("Category");
            appsGrid.setItems(role.getAllowedTools());
            appsGrid.setHeight("200px");
            mainLayout.add(createSection("Allowed Applications", appsGrid));
        }

        // Permissions
        if (role.getRolePermission() != null && !role.getRolePermission().isEmpty()) {
            Grid<RolePermissionDto> permsGrid = new Grid<>();
            permsGrid.addColumn(RolePermissionDto::getServiceName).setHeader("Service");
            permsGrid.addColumn(RolePermissionDto::getObjectName).setHeader("Object");
            permsGrid.addComponentColumn(perm -> new Span(perm.getRead() ? "✓" : "✗")).setHeader("Read (GET)");
            permsGrid.addComponentColumn(perm -> new Span(perm.getWrite() ? "✓" : "✗")).setHeader("Write (POST/PUT)");
            permsGrid.addComponentColumn(perm -> new Span(perm.getDelete() ? "✓" : "✗")).setHeader("Delete (DELETE)");
            permsGrid.setItems(role.getRolePermission());
            permsGrid.setHeight("300px");
            mainLayout.add(createSection("Permissions", permsGrid));
        }

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