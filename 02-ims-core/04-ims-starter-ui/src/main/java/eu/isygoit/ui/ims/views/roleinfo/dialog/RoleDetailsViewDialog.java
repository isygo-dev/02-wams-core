package eu.isygoit.ui.ims.views.roleinfo.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only view of a {@link RoleInfoDto}: every DTO field except {@code id}
 * (never displayed) is shown, grouped in tabs (identity, applications,
 * permissions, audit). {@code numberOfUsers} is computed and only shown here.
 */
public class RoleDetailsViewDialog extends ImsDetailsDialog {

    private final RoleManagementView parentView;
    private final RoleInfoService roleService;
    private final Long roleId;

    public RoleDetailsViewDialog(RoleManagementView parentView,
                                 RoleInfoService roleService,
                                 Long roleId) {
        super(I18n.t("ims.role.details.title"));
        this.parentView = parentView;
        this.roleService = roleService;
        this.roleId = roleId;

        applyWidth(DialogLayout.WIDTH_L);
        setModal(true);
        setDraggable(true);

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
            add(new Span(I18n.t("ims.role.details.load.error", RoleDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.role.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(RoleInfoDto role) {
        addIdentityTab(role);

        if (role.getAllowedTools() != null && !role.getAllowedTools().isEmpty()) {
            addTab(I18n.t("ims.role.details.section.apps"),
                    createSection(I18n.t("ims.role.details.section.apps"), buildAppsList(role.getAllowedTools())));
        }
        if (role.getRolePermission() != null && !role.getRolePermission().isEmpty()) {
            addTab(I18n.t("ims.role.details.section.perms"),
                    createSection(I18n.t("ims.role.details.section.perms"), buildPermissionsList(role.getRolePermission())));
        }

        addAuditTab(role.getCreatedBy(), formatDate(role.getCreateDate()),
                role.getUpdatedBy(), formatDate(role.getUpdateDate()));
    }

    private void addIdentityTab(RoleInfoDto role) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("ims.role.details.field.name"), dash(role.getName()));
        addFieldToGrid(grid, VaadinIcon.CODE, I18n.t("ims.role.details.field.code"), dash(role.getCode()), true);
        addFieldToGrid(grid, VaadinIcon.CLIPBOARD_TEXT, I18n.t("ims.role.details.field.template.code"),
                dash(role.getTemplateCode()), true);
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("ims.role.details.field.tenant"),
                dash(role.getTenant()), true);
        addFieldToGrid(grid, VaadinIcon.SORT, I18n.t("ims.role.details.field.level"), dash(role.getLevel()));
        addFieldToGrid(grid, VaadinIcon.USERS, I18n.t("ims.role.details.field.users"), dash(role.getNumberOfUsers()));
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("ims.role.details.field.description"),
                dash(role.getDescription()));

        VerticalLayout identity = DialogLayout.stack();
        identity.add(buildHero(role), createSection(I18n.t("ims.role.details.section.identity"), grid));
        addTab(I18n.t("ims.role.details.section.identity"), identity);
    }

    private Component buildHero(RoleInfoDto role) {
        List<Component> chips = new ArrayList<>();
        if (role.getCode() != null && !role.getCode().isBlank()) {
            Span codeChip = new Span(role.getCode());
            codeChip.addClassName(DialogLayout.CLASS_CHIP);
            chips.add(codeChip);
        }
        Span levelChip = new Span(I18n.t("ims.role.details.field.level") + ": " + dash(role.getLevel()));
        levelChip.addClassName(DialogLayout.CLASS_CHIP);
        chips.add(levelChip);
        return new DetailHero(null, dash(role.getName()), role.getDescription(), chips.toArray(Component[]::new));
    }

    private RowCardList<ApplicationDto> buildAppsList(List<ApplicationDto> apps) {
        return new RowCardList<ApplicationDto>()
                .emptyText("-")
                .cardFactory(app -> RowCard.create()
                        .title(app.getName())
                        .fact(I18n.t("ims.role.details.apps.column.title"), app.getTitle())
                        .fact(I18n.t("ims.role.details.apps.column.category"), app.getCategory()))
                .items(apps);
    }

    private RowCardList<RolePermissionDto> buildPermissionsList(List<RolePermissionDto> permissions) {
        return new RowCardList<RolePermissionDto>()
                .emptyText("-")
                .cardFactory(perm -> RowCard.create()
                        .title(perm.getObjectName())
                        .fact(I18n.t("ims.role.details.field.service"), perm.getServiceName())
                        .fact(I18n.t("ims.role.details.field.read"), flag(perm.getRead()))
                        .fact(I18n.t("ims.role.details.field.write"), flag(perm.getWrite()))
                        .fact(I18n.t("ims.role.details.field.delete"), flag(perm.getDelete())))
                .items(permissions);
    }

    private static String flag(Boolean value) {
        return Boolean.TRUE.equals(value) ? I18n.t("ims.role.details.yes") : I18n.t("ims.role.details.no");
    }

    private static String formatDate(LocalDateTime date) {
        return date == null ? null : DateHelper.formatToHumanReadable(date);
    }
}
