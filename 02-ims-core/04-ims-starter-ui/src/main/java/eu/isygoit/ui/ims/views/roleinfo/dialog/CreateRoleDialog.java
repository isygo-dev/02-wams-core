package eu.isygoit.ui.ims.views.roleinfo.dialog;

import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.dto.data.RolePermissionDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.ims.views.roleinfo.RoleManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Create dialog for {@link RoleInfoDto}. The form, applications grid and permission
 * tree live in {@link AbstractRoleFormDialog}; this class only creates the role.
 * Permissions can be added and removed; only complete ones (service and object) are sent.
 */
@Slf4j
public class CreateRoleDialog extends AbstractRoleFormDialog {

    public CreateRoleDialog(RoleManagementView parentView,
                            RoleInfoService roleInfoService,
                            ApplicationService applicationService,
                            Runnable onSuccess) {
        super(I18n.t("ims.role.dialog.create.title"), onSuccess, "ims.role.dialog.create",
                parentView, roleInfoService, applicationService);
        setOkButtonText(I18n.t("ims.role.dialog.create.button"));

        buildForm();
        levelField.setValue(0);
        loadInitialApplications();
    }

    private void loadInitialApplications() {
        parentView.showLoading(true);
        try {
            loadApplications();
        } catch (Exception e) {
            log.error("Failed to load applications", e);
            append(I18n.t("ims.role.dialog.apps.load.failed", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
    }

    @Override
    boolean permissionsEditable() {
        return true;
    }

    @Override
    List<RolePermissionDto> permissionsToSend() {
        return permissions.stream()
                .filter(p -> p.getServiceName() != null && !p.getServiceName().isBlank()
                        && p.getObjectName() != null && !p.getObjectName().isBlank())
                .collect(Collectors.toList());
    }

    @Override
    RoleInfoDto target() {
        return RoleInfoDto.builder().build();
    }

    @Override
    boolean persist(RoleInfoDto dto) {
        ResponseEntity<RoleInfoDto> response = roleService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.role.dialog.create.failed", response.getStatusCodeValue()));
            return false;
        }
        return true;
    }
}
