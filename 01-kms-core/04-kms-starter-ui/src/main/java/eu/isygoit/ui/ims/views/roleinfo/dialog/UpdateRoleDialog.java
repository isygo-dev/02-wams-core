package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UpdateRoleDialog extends BaseActionDialog {

    private final RoleManagementView parentView;
    private final RoleInfoService roleService;
    private final ApplicationService applicationService;
    private final RoleInfoDto role;
    private final Runnable onSuccess;

    // Basic info
    private TextField nameField;
    private TextField codeField;
    private IntegerField levelField;
    private TextArea descriptionField;

    // Allowed applications
    private Grid<ApplicationDto> availableAppsGrid;
    private Grid<ApplicationDto> assignedAppsGrid;
    private List<ApplicationDto> allApplications = new ArrayList<>();
    private Set<ApplicationDto> assignedApplications = new HashSet<>();

    // Permissions
    private Grid<RolePermissionDto> permissionsGrid;
    private List<RolePermissionDto> permissions = new ArrayList<>();

    private Tabs tabs;
    private VerticalLayout tabContent;

    public UpdateRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleService,
                            ApplicationService applicationService,
                            RoleInfoDto role,
                            Runnable onSuccess) {
        super("Edit Role");
        this.parentView = parentView;
        this.roleService = roleService;
        this.applicationService = applicationService;
        this.role = role;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("90%");
        getElement().getStyle().set("max-width", "1100px");
        setHeight("75vh");

        buildBasicForm();
        buildApplicationsGrid();
        buildPermissionsGrid();
        buildTabs();

        add(createMainLayout());
        loadApplicationsAndPopulate();
    }

    private void buildBasicForm() {
        nameField = new TextField("Role name *");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();
        codeField = new TextField("Role code *");
        codeField.setRequiredIndicatorVisible(true);
        codeField.setWidthFull();
        levelField = new IntegerField("Level");
        levelField.setWidthFull();
        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();

        // populate from existing role
        nameField.setValue(role.getName());
        codeField.setValue(role.getCode());
        levelField.setValue(role.getLevel());
        descriptionField.setValue(role.getDescription() != null ? role.getDescription() : "");
    }

    private void buildApplicationsGrid() {
        availableAppsGrid = new Grid<>();
        availableAppsGrid.addColumn(ApplicationDto::getName).setHeader("Name");
        availableAppsGrid.addColumn(ApplicationDto::getTitle).setHeader("Title");
        availableAppsGrid.addComponentColumn(app -> {
            Button addBtn = new Button(VaadinIcon.PLUS.create());
            addBtn.addClickListener(e -> {
                assignedApplications.add(app);
                refreshAppsGrids();
            });
            return addBtn;
        }).setHeader("Add").setWidth("70px");

        assignedAppsGrid = new Grid<>();
        assignedAppsGrid.addColumn(ApplicationDto::getName).setHeader("Name");
        assignedAppsGrid.addColumn(ApplicationDto::getTitle).setHeader("Title");
        assignedAppsGrid.addComponentColumn(app -> {
            Button removeBtn = new Button(VaadinIcon.MINUS.create());
            removeBtn.addClickListener(e -> {
                assignedApplications.remove(app);
                refreshAppsGrids();
            });
            return removeBtn;
        }).setHeader("Remove").setWidth("70px");

        // populate existing assigned apps
        if (role.getAllowedTools() != null) {
            assignedApplications.addAll(role.getAllowedTools());
        }
    }

    private void buildPermissionsGrid() {
        permissionsGrid = new Grid<>();
        permissionsGrid.addColumn(RolePermissionDto::getServiceName).setHeader("Service");
        permissionsGrid.addColumn(RolePermissionDto::getObjectName).setHeader("Object");
        permissionsGrid.addComponentColumn(perm -> {
            Checkbox readChk = new Checkbox(perm.getRead());
            readChk.addValueChangeListener(e -> perm.setRead(e.getValue()));
            return readChk;
        }).setHeader("Read (GET)").setWidth("100px");

        permissionsGrid.addComponentColumn(perm -> {
            Checkbox writeChk = new Checkbox(perm.getWrite());
            writeChk.addValueChangeListener(e -> perm.setWrite(e.getValue()));
            return writeChk;
        }).setHeader("Write (POST/PUT)").setWidth("120px");

        permissionsGrid.addComponentColumn(perm -> {
            Checkbox deleteChk = new Checkbox(perm.getDelete());
            deleteChk.addValueChangeListener(e -> perm.setDelete(e.getValue()));
            return deleteChk;
        }).setHeader("Delete (DELETE)").setWidth("100px");

        permissionsGrid.addComponentColumn(perm -> {
            Button removeBtn = new Button(VaadinIcon.TRASH.create());
            removeBtn.addClickListener(e -> {
                permissions.remove(perm);
                refreshPermissionsGrid();
            });
            return removeBtn;
        }).setHeader("Actions").setWidth("80px");

        if (role.getRolePermission() != null) {
            permissions.addAll(role.getRolePermission());
        }
        permissionsGrid.setItems(permissions);
    }

    private void refreshAppsGrids() {
        List<ApplicationDto> available = allApplications.stream()
                .filter(app -> !assignedApplications.contains(app))
                .collect(Collectors.toList());
        availableAppsGrid.setItems(available);
        assignedAppsGrid.setItems(new ArrayList<>(assignedApplications));
    }

    private void refreshPermissionsGrid() {
        permissionsGrid.setItems(new ArrayList<>(permissions));
    }

    private void loadApplicationsAndPopulate() {
        parentView.showLoading(true);
        try {
            ResponseEntity<PaginatedResponseDto<ApplicationDto>> response = applicationService.findAll(0, 100);
            if (response.getBody() != null && response.getBody().getContent() != null) {
                allApplications = response.getBody().getContent();
                refreshAppsGrids();
            }
        } catch (Exception e) {
            append("Failed to load applications: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildTabs() {
        tabs = new Tabs();
        tabContent = new VerticalLayout();
        tabContent.setPadding(false);
        tabContent.setSpacing(false);

        Tab basicTab = new Tab("Basic Info");
        Tab appsTab = new Tab("Allowed Applications");
        Tab permsTab = new Tab("Permissions");

        tabs.add(basicTab, appsTab, permsTab);
        tabs.addSelectedChangeListener(event -> updateTabContent(tabs.getSelectedIndex()));

        updateTabContent(0);
    }

    private void updateTabContent(int index) {
        tabContent.removeAll();
        switch (index) {
            case 0:
                VerticalLayout basicLayout = new VerticalLayout(nameField, codeField, levelField, descriptionField);
                basicLayout.setPadding(false);
                tabContent.add(basicLayout);
                break;
            case 1:
                HorizontalLayout appsLayout = new HorizontalLayout(availableAppsGrid, assignedAppsGrid);
                appsLayout.setSizeFull();
                availableAppsGrid.setSizeFull();
                assignedAppsGrid.setSizeFull();
                tabContent.add(appsLayout);
                break;
            case 2:
                VerticalLayout permsLayout = new VerticalLayout();
                Button addPermBtn = new Button("Add Permission", VaadinIcon.PLUS.create());
                addPermBtn.addClickListener(e -> showAddPermissionDialog());
                permsLayout.add(addPermBtn, permissionsGrid);
                permsLayout.setPadding(false);
                permsLayout.setHeightFull();
                permissionsGrid.setHeight("400px");
                tabContent.add(permsLayout);
                break;
        }
    }

    private void showAddPermissionDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add Permission");

        TextField serviceField = new TextField("Service name");
        TextField objectField = new TextField("Object name");
        Checkbox readChk = new Checkbox("Read (GET)");
        Checkbox writeChk = new Checkbox("Write (POST/PUT)");
        Checkbox deleteChk = new Checkbox("Delete (DELETE)");

        Button saveBtn = new Button("Add", e -> {
            RolePermissionDto newPerm = RolePermissionDto.builder()
                    .serviceName(serviceField.getValue())
                    .objectName(objectField.getValue())
                    .read(readChk.getValue())
                    .write(writeChk.getValue())
                    .delete(deleteChk.getValue())
                    .build();
            permissions.add(newPerm);
            refreshPermissionsGrid();
            dialog.close();
        });
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        VerticalLayout layout = new VerticalLayout(serviceField, objectField, readChk, writeChk, deleteChk);
        layout.setSpacing(true);
        dialog.add(layout);
        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private Component createMainLayout() {
        VerticalLayout main = new VerticalLayout(tabs, tabContent);
        main.setSizeFull();
        main.setPadding(false);
        main.setSpacing(false);
        return main;
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append("Role name is required");
            return false;
        }
        if (codeField.getValue().isBlank()) {
            append("Role code is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            role.setName(nameField.getValue());
            role.setCode(codeField.getValue());
            role.setLevel(levelField.getValue());
            role.setDescription(descriptionField.getValue());
            role.setAllowedTools(new ArrayList<>(assignedApplications));
            role.setRolePermission(permissions);

            ResponseEntity<RoleInfoDto> response = roleService.update(role.getId(), role);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: HTTP " + response.getStatusCodeValue());
                return false;
            }

            append("Role updated successfully");
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
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