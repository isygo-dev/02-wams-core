package eu.isygoit.ui.ims.views.tenant.dialog;

import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link TenantDto}. The form itself lives in
 * {@link AbstractTenantFormDialog}; this class only creates the tenant.
 */
public class CreateTenantDialog extends AbstractTenantFormDialog {

    public CreateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              TenantImageService tenantImageService,
                              Runnable onSuccess) {
        super(I18n.t("ims.tenant.dialog.create.title"), onSuccess, "ims.tenant.dialog.create",
                parentView, tenantService, tenantImageService);
        setOkButtonText(I18n.t("ims.tenant.dialog.create.button"));

        buildForm();
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
    }

    @Override
    String photoButtonKey() {
        return "ims.tenant.dialog.field.upload.image";
    }

    @Override
    boolean imageRequired() {
        return true;
    }

    @Override
    TenantDto target() {
        return TenantDto.builder().build();
    }

    @Override
    Long persist(TenantDto dto) {
        ResponseEntity<TenantDto> response = tenantService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.tenant.dialog.create.failed", response.getStatusCodeValue()));
            return null;
        }
        Long tenantId = response.getBody().getId();
        if (tenantId == null) {
            append(I18n.t("ims.tenant.dialog.create.no.id"));
            return null;
        }
        return tenantId;
    }
}
