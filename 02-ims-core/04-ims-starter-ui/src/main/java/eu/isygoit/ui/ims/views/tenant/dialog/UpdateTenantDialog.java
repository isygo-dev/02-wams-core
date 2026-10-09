package eu.isygoit.ui.ims.views.tenant.dialog;

import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link TenantDto}. The form itself lives in
 * {@link AbstractTenantFormDialog}; this class only loads and updates the tenant.
 * The tenant keeps the code of the loaded entity.
 */
@Slf4j
public class UpdateTenantDialog extends AbstractTenantFormDialog {

    private final TenantDto tenant;

    public UpdateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              TenantImageService tenantImageService,
                              TenantDto tenant,
                              Runnable onSuccess) {
        super(I18n.t("ims.tenant.dialog.update.title"), onSuccess, "ims.tenant.dialog.update",
                parentView, tenantService, tenantImageService);
        this.tenant = tenant;
        setOkButtonText(I18n.t("ims.tenant.dialog.update.button"));

        buildForm();
        fillFrom(tenant);
        loadExistingImage();
    }

    @Override
    String photoButtonKey() {
        return "ims.tenant.dialog.field.change.image";
    }

    @Override
    boolean imageRequired() {
        return false;
    }

    @Override
    TenantDto target() {
        return tenant;
    }

    @Override
    Long persist(TenantDto dto) {
        ResponseEntity<TenantDto> response = tenantService.update(dto.getId(), dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.tenant.dialog.update.failed", response.getStatusCodeValue()));
            return null;
        }
        return dto.getId();
    }

    private void loadExistingImage() {
        try {
            ResponseEntity<Resource> response = tenantImageService.downloadImage(tenant.getId());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                photoSection.showExisting(TenantDialogSupport.toDataUri(response.getBody()));
            }
        } catch (Exception e) {
            log.debug("No existing image for tenant {}", tenant.getId());
        }
    }
}
