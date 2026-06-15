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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
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

    // All applications grid with checkboxes
    private Grid<ApplicationDto> applicationsGrid;
    private TextField appsSearchField;
    private Span appsCountLabel;
    private List<ApplicationDto> allApplications = new ArrayList<>();
    private Set<Long> allowedApplicationIds = new HashSet<>();

    // Permissions – full matrix from backend
    private Grid<RolePermissionDto> permissionsGrid;
    private List<RolePermissionDto> allPermissions = new ArrayList<>();
    private TextField permSearchField;

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
        setWidth("95%");
        getElement().getStyle().set("max-width", "1100px");
        setHeight("85vh");
        setResizable(true);
        setDraggable(true);

        buildBasicForm();
        buildApplicationsGrid();
        buildPermissionsGrid();
        buildTabs();
        add(createMainLayout());
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

    private void buildPermissionsGrid() {
        permissionsGrid = new Grid<>();
        permissionsGrid.addColumn(RolePermissionDto::getServiceName).setHeader("Service").setAutoWidth(true);
        permissionsGrid.addColumn(RolePermissionDto::getObjectName).setHeader("Object").setAutoWidth(true);
        permissionsGrid.addComponentColumn(perm -> {
            Checkbox readChk = new Checkbox(perm.getRead() != null && perm.getRead());
            readChk.addValueChangeListener(e -> perm.setRead(e.getValue()));
            return readChk;
        }).setHeader("Read").setWidth("70px").setFlexGrow(0);
        permissionsGrid.addComponentColumn(perm -> {
            Checkbox writeChk = new Checkbox(perm.getWrite() != null && perm.getWrite());
            writeChk.addValueChangeListener(e -> perm.setWrite(e.getValue()));
            return writeChk;
        }).setHeader("Write").setWidth("70px").setFlexGrow(0);
        permissionsGrid.addComponentColumn(perm -> {
            Checkbox deleteChk = new Checkbox(perm.getDelete() != null && perm.getDelete());
            deleteChk.addValueChangeListener(e -> perm.setDelete(e.getValue()));
            return deleteChk;
        }).setHeader("Delete").setWidth("70px").setFlexGrow(0);
        permissionsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
        permissionsGrid.setHeight("350px");

        permSearchField = new TextField();
        permSearchField.setPlaceholder("Filter permissions...");
        permSearchField.setClearButtonVisible(true);
        permSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        permSearchField.addValueChangeListener(e -> filterPermissions());
    }

    private void filterPermissions() {
        String term = permSearchField.getValue().toLowerCase();
        List<RolePermissionDto> filtered = allPermissions.stream()
                .filter(p -> term.isEmpty() ||
                        (p.getServiceName() != null && p.getServiceName().toLowerCase().contains(term)) ||
                        (p.getObjectName() != null && p.getObjectName().toLowerCase().contains(term)))
                .collect(Collectors.toList());
        permissionsGrid.setItems(filtered);
    }

    private void refreshPermissionsGrid() {
        permissionsGrid.setItems(allPermissions);
        permSearchField.clear();
    }

    private void loadDataAndPopulate() {
        parentView.showLoading(true);
        try {
            // Fetch full role details (includes full permission matrix via afterFindById)
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
                log.info("Loaded {} allowed applications", allowedApplicationIds.size());
            }

            // Load all applications (with fallback)
            allApplications = fetchAllApplications();
            if (allApplications.isEmpty()) {
                Notification.show("No applications found. Please check the application service.", 5000, Notification.Position.BOTTOM_END);
                applicationsGrid.setItems(Collections.emptyList());
                appsCountLabel.setText("0 applications found");
            } else {
                log.info("Loaded {} total applications", allApplications.size());
                refreshApplicationsGrid();
            }

            // Permissions – full matrix is already in rolePermission
            if (fullRole.getRolePermission() != null && !fullRole.getRolePermission().isEmpty()) {
                allPermissions = fullRole.getRolePermission();
                log.info("Loaded {} permissions", allPermissions.size());
            } else {
                allPermissions = new ArrayList<>();
            }
            refreshPermissionsGrid();

        } catch (Exception e) {
            log.error("Error loading data", e);
            append("Failed to load data: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
    }

    private List<ApplicationDto> fetchAllApplications() {
        // Try 1: findAllListFull()
        try {
            ResponseEntity<List<ApplicationDto>> response = applicationService.findAllListFull();
            if (response.getBody() != null && !response.getBody().isEmpty()) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.debug("findAllListFull failed: {}", e.getMessage());
        }

        // Try 2: findAll(0, 1000) paginated
        try {
            ResponseEntity<PaginatedResponseDto<ApplicationDto>> paginated = applicationService.findAll(0, 1000);
            if (paginated.getBody() != null && paginated.getBody().getContent() != null) {
                return paginated.getBody().getContent();
            }
        } catch (Exception e) {
            log.debug("paginated findAll failed: {}", e.getMessage());
        }

        // Try 3: findAllList() via reflection (if exists)
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
                Notification.show("No applications found.", 5000, Notification.Position.BOTTOM_END);
                applicationsGrid.setItems(Collections.emptyList());
                appsCountLabel.setText("0 applications found");
            } else {
                refreshApplicationsGrid();
            }
        } catch (Exception e) {
            log.error("Failed to refresh applications", e);
            Notification.show("Error refreshing applications: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END);
        } finally {
            parentView.showLoading(false);
        }
    }

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
                VerticalLayout permsLayout = new VerticalLayout(permSearchField, permissionsGrid);
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
                    .allowed-apps-layout {
                        flex-direction: column !important;
                    }
                    .allowed-apps-layout > vaadin-horizontal-layout {
                        flex-direction: column !important;
                    }
                    .button-column {
                        flex-direction: row !important;
                        justify-content: center;
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