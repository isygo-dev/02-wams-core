package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Shared form of the role create/update dialogs: the editable {@link RoleInfoDto}
 * fields (basic info), the allowed-applications grid and the permission matrix are
 * declared here once, in three tabs. {@code id} is never shown, {@code code} is
 * always read-only and {@code numberOfUsers} (computed) is only shown in the details.
 * Subclasses decide whether permissions can be added/removed and how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
@Slf4j
abstract class AbstractRoleFormDialog extends ImsActionDialog {

    final RoleManagementView parentView;
    final RoleInfoService roleService;
    final ApplicationService applicationService;
    private final String messagePrefix;

    /* basic info */
    TextField codeField;
    TextField nameField;
    IntegerField levelField;
    TextField tenantField;
    TextField templateCodeField;
    TextArea descriptionField;

    /* allowed applications */
    RowCardList<ApplicationDto> applicationsGrid;
    private TextField appsSearchField;
    private Span appsCountLabel;
    List<ApplicationDto> allApplications = new ArrayList<>();
    Set<Long> allowedApplicationIds = new HashSet<>();

    /* permissions */
    RowCardList<RolePermissionDto> permissionsTree;
    List<RolePermissionDto> permissions = new ArrayList<>();
    private TextField permSearchField;

    AbstractRoleFormDialog(String title,
                           Runnable onSuccess,
                           String messagePrefix,
                           RoleManagementView parentView,
                           RoleInfoService roleService,
                           ApplicationService applicationService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.roleService = roleService;
        this.applicationService = applicationService;
    }

    /* Hooks */

    /** Whether permissions can be added and removed (create) or only toggled (update). */
    abstract boolean permissionsEditable();

    /** The permissions sent to the server on save. */
    abstract List<RolePermissionDto> permissionsToSend();

    /** The DTO that receives the form values on save. */
    abstract RoleInfoDto target();

    /** Persists the DTO; returns false after appending an error. */
    abstract boolean persist(RoleInfoDto dto);

    /* Layout */

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_L);
        setDraggable(true);

        buildBasicFields();
        buildApplicationsGrid();
        buildPermissionsGrid();

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add(new Tab(I18n.t("ims.role.dialog.tab.basic")), buildBasicTab());
        tabs.add(new Tab(I18n.t("ims.role.dialog.tab.apps")), buildApplicationsTab());
        tabs.add(new Tab(I18n.t("ims.role.dialog.tab.perms")), buildPermissionsTab());
        addContent(tabs);
    }

    private void buildBasicFields() {
        codeField = new TextField(I18n.t("ims.role.details.field.code"));
        codeField.setReadOnly(true);

        nameField = new TextField(I18n.t("ims.role.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);

        levelField = new IntegerField(I18n.t("ims.role.dialog.field.level"));
        levelField.setStepButtonsVisible(true);

        tenantField = new TextField(I18n.t("ims.role.dialog.field.tenant"));
        templateCodeField = new TextField(I18n.t("ims.role.dialog.field.template.code"));

        descriptionField = DialogLayout.tall(new TextArea(I18n.t("ims.role.dialog.field.description")));
    }

    private Component buildBasicTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.role.details.section.identity"), VaadinIcon.KEY);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(nameField, codeField);
        form.add(levelField, tenantField);
        form.add(templateCodeField, 2);
        form.add(descriptionField, 2);
        section.add(form);
        return section;
    }

    private Component buildApplicationsTab() {
        Button refreshAppsBtn = new Button(I18n.t("ims.role.dialog.apps.refresh"), VaadinIcon.REFRESH.create());
        refreshAppsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        refreshAppsBtn.addClickListener(e -> refreshApplications());

        HorizontalLayout topBar = new HorizontalLayout(appsSearchField, refreshAppsBtn, appsCountLabel);
        topBar.setWidthFull();
        topBar.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topBar.expand(appsSearchField);

        VerticalLayout tab = DialogLayout.stack();
        tab.add(topBar, applicationsGrid);
        return tab;
    }

    private Component buildPermissionsTab() {
        HorizontalLayout topBar = new HorizontalLayout(permSearchField);
        if (permissionsEditable()) {
            Button addPermBtn = new Button(I18n.t("ims.role.dialog.apps.add.permission"), VaadinIcon.PLUS.create());
            addPermBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addPermBtn.addClickListener(e -> {
                permissions.add(RolePermissionDto.builder()
                        .serviceName("")
                        .objectName("")
                        .read(false)
                        .write(false)
                        .delete(false)
                        .build());
                refreshPermissionsTree();
            });
            topBar.add(addPermBtn);
        }
        topBar.setWidthFull();
        topBar.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        topBar.expand(permSearchField);

        VerticalLayout tab = DialogLayout.stack();
        tab.add(topBar, permissionsTree);
        return tab;
    }

    /* Applications grid (all apps with checkboxes) */

    private void buildApplicationsGrid() {
        applicationsGrid = new RowCardList<ApplicationDto>()
                .emptyText("-")
                .cardFactory(app -> {
                    Checkbox cb = new Checkbox(allowedApplicationIds.contains(app.getId()));
                    cb.setAriaLabel(I18n.t("ims.role.dialog.apps.column.allow"));
                    cb.addValueChangeListener(e -> {
                        if (Boolean.TRUE.equals(e.getValue())) {
                            allowedApplicationIds.add(app.getId());
                        } else {
                            allowedApplicationIds.remove(app.getId());
                        }
                    });
                    return RowCard.create()
                            .title(app.getName())
                            .fact(I18n.t("ims.role.dialog.apps.column.title"), app.getTitle())
                            .control(cb);
                });

        appsSearchField = new TextField();
        appsSearchField.setPlaceholder(I18n.t("ims.role.dialog.apps.search"));
        appsSearchField.setAriaLabel(I18n.t("ims.role.dialog.apps.search"));
        appsSearchField.setClearButtonVisible(true);
        appsSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        appsSearchField.addValueChangeListener(e -> filterApplicationsGrid());

        appsCountLabel = new Span();
        appsCountLabel.addClassName(DialogLayout.CLASS_HELP);
    }

    private void filterApplicationsGrid() {
        String term = appsSearchField.getValue() == null ? "" : appsSearchField.getValue().toLowerCase();
        List<ApplicationDto> filtered = allApplications.stream()
                .filter(app -> term.isEmpty() ||
                        app.getName().toLowerCase().contains(term) ||
                        app.getTitle().toLowerCase().contains(term))
                .collect(Collectors.toList());
        applicationsGrid.setItems(filtered);
        appsCountLabel.setText(I18n.t("ims.role.dialog.apps.count", filtered.size()));
    }

    private void refreshApplicationsGrid() {
        applicationsGrid.setItems(allApplications);
        appsCountLabel.setText(I18n.t("ims.role.dialog.apps.total", allApplications.size()));
        appsSearchField.clear();
    }

    /* Permissions TreeGrid (grouped by service) */

    private void buildPermissionsGrid() {
        permissionsTree = new RowCardList<RolePermissionDto>()
                .emptyText("-")
                .cardFactory(perm -> {
                    RowCard card = RowCard.create()
                        .title(perm.getObjectName())
                        .fact(I18n.t("ims.role.dialog.perms.column.service"), perm.getServiceName())
                        .control(permissionBox(perm.getRead(), "ims.role.dialog.perms.column.read", perm::setRead))
                        .control(permissionBox(perm.getWrite(), "ims.role.dialog.perms.column.write", perm::setWrite))
                        .control(permissionBox(perm.getDelete(), "ims.role.dialog.perms.column.delete", perm::setDelete));
                    if (permissionsEditable()) {
                        card.action(VaadinIcon.TRASH, I18n.t("ims.role.dialog.perms.remove"), () -> {
                            permissions.remove(perm);
                            refreshPermissionsTree();
                        });
                    }
                    return card;
                });

        refreshPermissionsTree();

        permSearchField = new TextField();
        permSearchField.setPlaceholder(I18n.t("ims.role.dialog.perms.search"));
        permSearchField.setAriaLabel(I18n.t("ims.role.dialog.perms.search"));
        permSearchField.setClearButtonVisible(true);
        permSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        permSearchField.addValueChangeListener(e -> filterPermissionsTree());
    }

    private static Checkbox permissionBox(Boolean value, String labelKey, Consumer<Boolean> setter) {
        Checkbox chk = new Checkbox(I18n.t(labelKey), Boolean.TRUE.equals(value));
        chk.addValueChangeListener(e -> setter.accept(e.getValue()));
        return chk;
    }

    final void refreshPermissionsTree() {
        permissionsTree.setItems(groupedByService(permissions));
    }

    private void filterPermissionsTree() {
        String term = permSearchField.getValue().toLowerCase();
        if (term.isEmpty()) {
            refreshPermissionsTree();
            return;
        }
        List<RolePermissionDto> filtered = new ArrayList<>();
        Map<String, List<RolePermissionDto>> grouped = permissions.stream()
                .collect(Collectors.groupingBy(RolePermissionDto::getServiceName,
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<String, List<RolePermissionDto>> entry : grouped.entrySet()) {
            String svc = entry.getKey();
            List<RolePermissionDto> matching = entry.getValue().stream()
                    .filter(p -> p.getServiceName().toLowerCase().contains(term) ||
                            p.getObjectName().toLowerCase().contains(term))
                    .collect(Collectors.toList());
            if (!matching.isEmpty() || svc.toLowerCase().contains(term)) {
                filtered.addAll(matching.isEmpty() ? entry.getValue() : matching);
            }
        }
        permissionsTree.setItems(filtered);
    }

    /** Permissions regrouped by service, in first-appearance order of each service. */
    private static List<RolePermissionDto> groupedByService(List<RolePermissionDto> source) {
        return source.stream()
                .collect(Collectors.groupingBy(RolePermissionDto::getServiceName,
                        LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /* Applications loading */

    /** Reloads every application and rebinds the grid; the checked ids are kept. */
    final void loadApplications() {
        allApplications = fetchAllApplications();
        if (allApplications.isEmpty()) {
            Notification.show(I18n.t("ims.role.dialog.apps.no.apps"), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            applicationsGrid.setItems(Collections.emptyList());
            appsCountLabel.setText(I18n.t("ims.role.dialog.apps.none.found"));
        } else {
            refreshApplicationsGrid();
        }
    }

    private void refreshApplications() {
        parentView.showLoading(true);
        try {
            loadApplications();
        } catch (Exception e) {
            log.error("Failed to refresh applications", e);
            Notification.show(I18n.t("ims.role.dialog.update.apps.refresh.error", e.getMessage()),
                            5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
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
            ResponseEntity<List<ApplicationDto>> listResp =
                    (ResponseEntity<List<ApplicationDto>>) method.invoke(applicationService);
            if (listResp.getBody() != null && !listResp.getBody().isEmpty()) {
                return listResp.getBody();
            }
        } catch (Exception e) {
            log.debug("findAllList failed: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    /* DTO <-> form */

    /** Fills the basic fields from the DTO (update flow). */
    final void fillBasicFrom(RoleInfoDto dto) {
        codeField.setValue(dto.getCode() != null ? dto.getCode() : "");
        nameField.setValue(dto.getName() != null ? dto.getName() : "");
        levelField.setValue(dto.getLevel());
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        templateCodeField.setValue(dto.getTemplateCode() != null ? dto.getTemplateCode() : "");
        descriptionField.setValue(dto.getDescription() != null ? dto.getDescription() : "");
    }

    /** Copies the editable values into the DTO. {@code id} and {@code code} are left untouched. */
    private void applyTo(RoleInfoDto dto) {
        dto.setName(nameField.getValue());
        dto.setLevel(levelField.getValue());
        if (!tenantField.getValue().isBlank()) {
            dto.setTenant(tenantField.getValue().trim());
        }
        if (!templateCodeField.getValue().isBlank()) {
            dto.setTemplateCode(templateCodeField.getValue().trim());
        }
        dto.setDescription(descriptionField.getValue());
        dto.setAllowedTools(allApplications.stream()
                .filter(app -> allowedApplicationIds.contains(app.getId()))
                .collect(Collectors.toList()));
        dto.setRolePermission(permissionsToSend());
    }

    /* Save */

    @Override
    protected final boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.role.dialog.field.name.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            RoleInfoDto dto = target();
            applyTo(dto);

            if (!persist(dto)) {
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            log.error("Feign error", ex);
            append(RoleDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            log.error("Error saving role", e);
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
