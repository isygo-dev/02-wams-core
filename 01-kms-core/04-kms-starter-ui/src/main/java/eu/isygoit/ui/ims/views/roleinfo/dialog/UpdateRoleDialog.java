package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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

    // Applications grid (all apps with checkbox)
    private Grid<ApplicationDto> applicationsGrid;
    private TextField appsSearchField;
    private Span appsCountLabel;
    private List<ApplicationDto> allApplications = new ArrayList<>();
    private Set<Long> allowedApplicationIds = new HashSet<>();

    // Permissions TreeGrid (full matrix from backend)
    private TreeGrid<Object> permissionsTree;
    private List<RolePermissionDto> allPermissions = new ArrayList<>();
    private TextField permSearchField;

    private Tabs tabs;
    private VerticalLayout tabContent;

    public UpdateRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleService,
                            ApplicationService applicationService,
                            RoleInfoDto role,
                            Runnable onSuccess) {
        super("Edit Role", onSuccess);
        this.parentView = parentView;
        this.roleService = roleService;
        this.applicationService = applicationService;
        this.role = role;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("95%");
        setMaxWidth("1100px");
        setHeight("85vh");
        setResizable(true);
        setDraggable(true);

        buildBasicForm();
        buildApplicationsGrid();
        buildPermissionsGrid();
        buildTabs();
        addContent(createMainLayout());
        loadDataAndPopulate();

        injectResponsiveStyles();
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
        descriptionField.setHeight("80px");
    }

    // ------------------------------------------------------------------------
    // Applications grid (all apps with checkboxes)
    // ------------------------------------------------------------------------
    private void buildApplicationsGrid() {
        applicationsGrid = new Grid<>();
        applicationsGrid.addComponentColumn(app -> {
            Checkbox cb = new Checkbox(allowedApplicationIds.contains(app.getId()));
            cb.addValueChangeListener(e -> {
                if (Boolean.TRUE.equals(e.getValue())) {
                    allowedApplicationIds.add(app.getId());
                } else {
                    allowedApplicationIds.remove(app.getId());
                }
            });
            return cb;
        }).setHeader("Allow").setWidth("70px").setFlexGrow(0);
        applicationsGrid.addColumn(ApplicationDto::getName).setHeader("Name").setAutoWidth(true);
        applicationsGrid.addColumn(ApplicationDto::getTitle).setHeader("Title").setAutoWidth(true);
        applicationsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
        applicationsGrid.setHeight("350px");

        appsSearchField = new TextField();
        appsSearchField.setPlaceholder("Search applications...");
        appsSearchField.setClearButtonVisible(true);
        appsSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        appsSearchField.addValueChangeListener(e -> filterApplicationsGrid());

        appsCountLabel = new Span();
        appsCountLabel.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        appsCountLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
    }

    private void filterApplicationsGrid() {
        String term = appsSearchField.getValue() == null ? "" : appsSearchField.getValue().toLowerCase();
        List<ApplicationDto> filtered = allApplications.stream()
                .filter(app -> term.isEmpty() ||
                        app.getName().toLowerCase().contains(term) ||
                        app.getTitle().toLowerCase().contains(term))
                .collect(Collectors.toList());
        applicationsGrid.setItems(filtered);
        appsCountLabel.setText(filtered.size() + " applications shown");
    }

    private void refreshApplicationsGrid() {
        applicationsGrid.setItems(allApplications);
        appsCountLabel.setText(allApplications.size() + " total applications");
        appsSearchField.clear();
    }

    // ------------------------------------------------------------------------
    // Permissions TreeGrid (grouped by service) – full matrix, no add/remove
    // ------------------------------------------------------------------------
    private void buildPermissionsGrid() {
        permissionsTree = new TreeGrid<>();
        permissionsTree.addHierarchyColumn(item -> {
            if (item instanceof String) return (String) item;
            if (item instanceof RolePermissionDto) return ((RolePermissionDto) item).getObjectName();
            return "";
        }).setHeader("Service / Object").setAutoWidth(true);

        permissionsTree.addComponentColumn(item -> {
            if (item instanceof RolePermissionDto) {
                Checkbox chk = new Checkbox(((RolePermissionDto) item).getRead());
                chk.addValueChangeListener(e -> ((RolePermissionDto) item).setRead(e.getValue()));
                return chk;
            }
            return new Span();
        }).setHeader("Read").setWidth("70px").setFlexGrow(0);

        permissionsTree.addComponentColumn(item -> {
            if (item instanceof RolePermissionDto) {
                Checkbox chk = new Checkbox(((RolePermissionDto) item).getWrite());
                chk.addValueChangeListener(e -> ((RolePermissionDto) item).setWrite(e.getValue()));
                return chk;
            }
            return new Span();
        }).setHeader("Write").setWidth("70px").setFlexGrow(0);

        permissionsTree.addComponentColumn(item -> {
            if (item instanceof RolePermissionDto) {
                Checkbox chk = new Checkbox(((RolePermissionDto) item).getDelete());
                chk.addValueChangeListener(e -> ((RolePermissionDto) item).setDelete(e.getValue()));
                return chk;
            }
            return new Span();
        }).setHeader("Delete").setWidth("70px").setFlexGrow(0);

        // No remove button in update dialog – the matrix is fixed

        permissionsTree.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
        permissionsTree.setHeight("350px");

        refreshPermissionsTree();

        permSearchField = new TextField();
        permSearchField.setPlaceholder("Filter permissions...");
        permSearchField.setClearButtonVisible(true);
        permSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        permSearchField.addValueChangeListener(e -> filterPermissionsTree());
    }

    private void refreshPermissionsTree() {
        TreeData<Object> treeData = new TreeData<>();
        Map<String, List<RolePermissionDto>> grouped = allPermissions.stream()
                .collect(Collectors.groupingBy(RolePermissionDto::getServiceName,
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<RolePermissionDto>> entry : grouped.entrySet()) {
            String serviceName = entry.getKey();
            treeData.addItem(null, serviceName);
            for (RolePermissionDto perm : entry.getValue()) {
                treeData.addItem(serviceName, perm);
            }
        }
        permissionsTree.setTreeData(treeData);
    }

    private void filterPermissionsTree() {
        String term = permSearchField.getValue().toLowerCase();
        if (term.isEmpty()) {
            refreshPermissionsTree();
            return;
        }
        TreeData<Object> filtered = new TreeData<>();
        Map<String, List<RolePermissionDto>> grouped = allPermissions.stream()
                .collect(Collectors.groupingBy(RolePermissionDto::getServiceName));
        for (Map.Entry<String, List<RolePermissionDto>> entry : grouped.entrySet()) {
            String svc = entry.getKey();
            List<RolePermissionDto> matching = entry.getValue().stream()
                    .filter(p -> p.getServiceName().toLowerCase().contains(term) ||
                            p.getObjectName().toLowerCase().contains(term))
                    .collect(Collectors.toList());
            if (!matching.isEmpty() || svc.toLowerCase().contains(term)) {
                filtered.addItem(null, svc);
                List<RolePermissionDto> children = matching.isEmpty() ? entry.getValue() : matching;
                for (RolePermissionDto p : children) {
                    filtered.addItem(svc, p);
                }
            }
        }
        permissionsTree.setTreeData(filtered);
    }

    // ------------------------------------------------------------------------
    // Data loading
    // ------------------------------------------------------------------------
    private void loadDataAndPopulate() {
        parentView.showLoading(true);
        try {
            // Fetch role details (full permission matrix from afterFindById)
            ResponseEntity<RoleInfoDto> fullRoleResp = roleService.findById(role.getId());
            if (fullRoleResp.getBody() == null) {
                append("Role not found");
                return;
            }
            RoleInfoDto fullRole = fullRoleResp.getBody();
            log.info("Loaded role: {}", fullRole.getName());

            // Basic info
            nameField.setValue(fullRole.getName());
            codeField.setValue(fullRole.getCode());
            levelField.setValue(fullRole.getLevel());
            descriptionField.setValue(fullRole.getDescription() != null ? fullRole.getDescription() : "");

            // Allowed applications – pre‑populate checked IDs
            if (fullRole.getAllowedTools() != null && !fullRole.getAllowedTools().isEmpty()) {
                allowedApplicationIds = fullRole.getAllowedTools().stream()
                        .map(ApplicationDto::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }

            // Load all applications
            allApplications = fetchAllApplications();
            if (allApplications.isEmpty()) {
                Notification.show("No applications found.", 5000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                applicationsGrid.setItems(Collections.emptyList());
                appsCountLabel.setText("0 applications found");
            } else {
                refreshApplicationsGrid();
            }

            // Permissions – full matrix
            if (fullRole.getRolePermission() != null && !fullRole.getRolePermission().isEmpty()) {
                allPermissions = fullRole.getRolePermission();
                refreshPermissionsTree();
            } else {
                allPermissions = new ArrayList<>();
                refreshPermissionsTree();
            }

        } catch (Exception e) {
            log.error("Error loading data", e);
            append("Failed to load data: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
    }

    private List<ApplicationDto> fetchAllApplications() {
        // Try findAllListFull()
        try {
            ResponseEntity<List<ApplicationDto>> response = applicationService.findAllListFull();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.debug("findAllListFull failed: {}", e.getMessage());
        }

        // Fallback to paginated findAll
        try {
            ResponseEntity<PaginatedResponseDto<ApplicationDto>> paginated = applicationService.findAll(0, 1000);
            if (paginated.getBody() != null && paginated.getBody().getContent() != null) {
                return paginated.getBody().getContent();
            }
        } catch (Exception e) {
            log.debug("paginated findAll failed: {}", e.getMessage());
        }

        // Fallback to findAllList via reflection
        try {
            var method = applicationService.getClass().getMethod("findAllList");
            @SuppressWarnings("unchecked")
            ResponseEntity<List<ApplicationDto>> listResp = (ResponseEntity<List<ApplicationDto>>) method.invoke(applicationService);
            if (listResp.getBody() != null && !listResp.getBody().isEmpty()) {
                return listResp.getBody();
            }
        } catch (Exception e) {
            log.debug("findAllList failed: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    private void refreshApplications() {
        parentView.showLoading(true);
        try {
            allApplications = fetchAllApplications();
            if (allApplications.isEmpty()) {
                Notification.show("No applications found.", 5000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                applicationsGrid.setItems(Collections.emptyList());
                appsCountLabel.setText("0 applications found");
            } else {
                refreshApplicationsGrid();
            }
        } catch (Exception e) {
            log.error("Failed to refresh applications", e);
            Notification.show("Error refreshing applications: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }
    }

    // ------------------------------------------------------------------------
    // Tab & layout
    // ------------------------------------------------------------------------
    private void buildTabs() {
        tabs = new Tabs();
        tabContent = new VerticalLayout();
        tabContent.setPadding(false);
        tabContent.setSpacing(false);
        tabContent.setSizeFull();

        Tab basicTab = new Tab("Basic Info");
        Tab appsTab = new Tab("Applications");
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
                basicLayout.setSpacing(false);
                tabContent.add(basicLayout);
                break;
            case 1:
                VerticalLayout appsLayout = new VerticalLayout();
                appsLayout.setPadding(false);
                appsLayout.setSpacing(false);
                Button refreshAppsBtn = new Button("Refresh", VaadinIcon.REFRESH.create());
                refreshAppsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
                refreshAppsBtn.addClickListener(e -> refreshApplications());
                HorizontalLayout topBar = new HorizontalLayout(appsSearchField, refreshAppsBtn, appsCountLabel);
                topBar.setWidthFull();
                topBar.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
                topBar.expand(appsSearchField);
                appsLayout.add(topBar, applicationsGrid);
                tabContent.add(appsLayout);
                break;
            case 2:
                VerticalLayout permsLayout = new VerticalLayout(permSearchField, permissionsTree);
                permsLayout.setPadding(false);
                permsLayout.setSpacing(false);
                permsLayout.setHeightFull();
                tabContent.add(permsLayout);
                break;
        }
    }

    private Component createMainLayout() {
        VerticalLayout main = new VerticalLayout(tabs, tabContent);
        main.setSizeFull();
        main.setPadding(false);
        main.setSpacing(false);
        return main;
    }

    private void injectResponsiveStyles() {
        String css = """
                @media (max-width: 600px) {
                    .apps-filter-bar {
                        flex-direction: column;
                        gap: 8px;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
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

            // Build allowed tools list from checked IDs
            List<ApplicationDto> allowedTools = allApplications.stream()
                    .filter(app -> allowedApplicationIds.contains(app.getId()))
                    .collect(Collectors.toList());
            role.setAllowedTools(allowedTools);

            // Send only permissions that have at least one flag true
            List<RolePermissionDto> toSend = allPermissions.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getRead()) ||
                            Boolean.TRUE.equals(p.getWrite()) ||
                            Boolean.TRUE.equals(p.getDelete()))
                    .collect(Collectors.toList());
            role.setRolePermission(toSend);
            log.info("Saving role with {} allowed apps and {} permissions", allowedTools.size(), toSend.size());

            ResponseEntity<RoleInfoDto> response = roleService.update(role.getId(), role);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: HTTP " + response.getStatusCodeValue());
                return false;
            }
            append("Role updated successfully");
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            log.error("Feign error", ex);
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            log.error("Error", e);
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