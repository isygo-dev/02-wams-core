package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
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
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("ims.role.details.title"));
        this.parentView = parentView;
        this.roleService = roleService;
        this.roleId = roleId;

        setWidth("900px");
        setMaxWidth("95%");
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
                add(new Span(I18n.t("ims.role.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.role.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.role.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(RoleInfoDto role) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — name/code/template code (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.role.details.field.name"), role.getName());
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.role.details.field.code"), role.getCode());
        addFieldToGrid(identityInfo, VaadinIcon.CLIPBOARD_TEXT, I18n.t("ims.role.details.field.template.code"), role.getTemplateCode());

        mainLayout.add(createSection(I18n.t("ims.role.details.section.identity"), identityInfo));

        // Classification & status — level/number of users
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.SORT, I18n.t("ims.role.details.field.level"), String.valueOf(role.getLevel()));
        addFieldToGrid(classificationInfo, VaadinIcon.USERS, I18n.t("ims.role.details.field.users"), String.valueOf(role.getNumberOfUsers()));

        mainLayout.add(createSection(I18n.t("ims.role.details.section.classification"), classificationInfo));

        // Contact / relations — tenant/description
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.role.details.field.tenant"), role.getTenant());

        mainLayout.add(createSection(I18n.t("ims.role.details.section.contact"), contactInfo));

        if (role.getDescription() != null && !role.getDescription().isBlank()) {
            Div descGrid = new Div();
            descGrid.addClassName("wams-card__detail-grid");
            addFieldToGrid(descGrid, VaadinIcon.FILE_TEXT, I18n.t("ims.role.details.field.description"), role.getDescription());
            mainLayout.add(descGrid);
        }

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.role.details.field.created"), role.getCreateDate() != null ? DateHelper.formatToHumanReadable(role.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.role.details.field.created.by"), role.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.role.details.field.updated"), role.getUpdateDate() != null ? DateHelper.formatToHumanReadable(role.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.role.details.field.updated.by"), role.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.role.details.section.audit"), auditInfo));

        // Allowed Applications
        if (role.getAllowedTools() != null && !role.getAllowedTools().isEmpty()) {
            Grid<ApplicationDto> appsGrid = new Grid<>();
            appsGrid.addColumn(ApplicationDto::getName).setHeader(I18n.t("ims.role.details.apps.column.name"));
            appsGrid.addColumn(ApplicationDto::getTitle).setHeader(I18n.t("ims.role.details.apps.column.title"));
            appsGrid.addColumn(ApplicationDto::getCategory).setHeader(I18n.t("ims.role.details.apps.column.category"));
            appsGrid.setItems(role.getAllowedTools());
            appsGrid.setHeight("200px");
            mainLayout.add(createSection(I18n.t("ims.role.details.section.apps"), appsGrid));
        }

        // Permissions
        if (role.getRolePermission() != null && !role.getRolePermission().isEmpty()) {
            Grid<RolePermissionDto> permsGrid = new Grid<>();
            permsGrid.addColumn(RolePermissionDto::getServiceName).setHeader(I18n.t("ims.role.details.field.service"));
            permsGrid.addColumn(RolePermissionDto::getObjectName).setHeader(I18n.t("ims.role.details.field.object"));
            permsGrid.addComponentColumn(perm -> new Span(perm.getRead() ? I18n.t("ims.role.details.yes") : I18n.t("ims.role.details.no"))).setHeader(I18n.t("ims.role.details.field.read"));
            permsGrid.addComponentColumn(perm -> new Span(perm.getWrite() ? I18n.t("ims.role.details.yes") : I18n.t("ims.role.details.no"))).setHeader(I18n.t("ims.role.details.field.write"));
            permsGrid.addComponentColumn(perm -> new Span(perm.getDelete() ? I18n.t("ims.role.details.yes") : I18n.t("ims.role.details.no"))).setHeader(I18n.t("ims.role.details.field.delete"));
            permsGrid.setItems(role.getRolePermission());
            permsGrid.setHeight("300px");
            mainLayout.add(createSection(I18n.t("ims.role.details.section.perms"), permsGrid));
        }

        add(mainLayout);
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;

        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, labelSpan);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        container.add(field);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
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