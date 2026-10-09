package eu.isygoit.ui.ims.views.roleinfo.dialog;

import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Update dialog for {@link RoleInfoDto}. The form, applications grid and permission
 * tree live in {@link AbstractRoleFormDialog}; this class loads the full role
 * (permission matrix included) and updates it. The permission matrix is fixed: only
 * the flags can be toggled, and only permissions with at least one flag are sent.
 */
@Slf4j
public class UpdateRoleDialog extends AbstractRoleFormDialog {

    private final RoleInfoDto role;

    public UpdateRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleService,
                            ApplicationService applicationService,
                            RoleInfoDto role,
                            Runnable onSuccess) {
        super(I18n.t("ims.role.dialog.update.title"), onSuccess, "ims.role.dialog.update",
                parentView, roleService, applicationService);
        this.role = role;
        setOkButtonText(I18n.t("ims.role.dialog.update.button"));

        buildForm();
        loadDataAndPopulate();
    }

    private void loadDataAndPopulate() {
        parentView.showLoading(true);
        try {
            // Fetch role details (full permission matrix from afterFindById)
            ResponseEntity<RoleInfoDto> fullRoleResp = roleService.findById(role.getId());
            if (fullRoleResp.getBody() == null) {
                append(I18n.t("ims.role.dialog.update.not.found"));
                return;
            }
            RoleInfoDto fullRole = fullRoleResp.getBody();
            log.info("Loaded role: {}", fullRole.getName());

            fillBasicFrom(fullRole);

            // Allowed applications: pre-populate the checked ids, then load every application
            if (fullRole.getAllowedTools() != null && !fullRole.getAllowedTools().isEmpty()) {
                allowedApplicationIds = fullRole.getAllowedTools().stream()
                        .map(ApplicationDto::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
            loadApplications();

            // Permissions: full matrix
            permissions = fullRole.getRolePermission() != null && !fullRole.getRolePermission().isEmpty()
                    ? fullRole.getRolePermission()
                    : new ArrayList<>();
            refreshPermissionsTree();
        } catch (Exception e) {
            log.error("Error loading data", e);
            append(I18n.t("ims.role.dialog.update.load.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
    }

    @Override
    boolean permissionsEditable() {
        return false;
    }

    @Override
    List<RolePermissionDto> permissionsToSend() {
        List<RolePermissionDto> toSend = permissions.stream()
                .filter(p -> Boolean.TRUE.equals(p.getRead())
                        || Boolean.TRUE.equals(p.getWrite())
                        || Boolean.TRUE.equals(p.getDelete()))
                .collect(Collectors.toList());
        log.info("Saving role with {} allowed apps and {} permissions",
                allowedApplicationIds.size(), toSend.size());
        return toSend;
    }

    @Override
    RoleInfoDto target() {
        return role;
    }

    @Override
    boolean persist(RoleInfoDto dto) {
        ResponseEntity<RoleInfoDto> response = roleService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.role.dialog.update.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
