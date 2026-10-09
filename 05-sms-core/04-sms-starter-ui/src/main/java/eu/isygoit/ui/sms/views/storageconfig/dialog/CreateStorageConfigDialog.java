package eu.isygoit.ui.sms.views.storageconfig.dialog;

import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link StorageConfigDto}. The form itself lives in
 * {@link AbstractStorageConfigFormDialog}; this class only creates the configuration.
 */
public class CreateStorageConfigDialog extends AbstractStorageConfigFormDialog {

    public CreateStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     Runnable onSuccess) {
        super(I18n.t("sms.storageconfig.dialog.create.title"), onSuccess,
                "sms.storageconfig.dialog.create", true, parentView, storageConfigService);
        setOkButtonText(I18n.t("sms.storageconfig.dialog.create.button"));

        buildForm();
    }

    @Override
    StorageConfigDto target() {
        return new StorageConfigDto();
    }

    @Override
    ResponseEntity<StorageConfigDto> persist(StorageConfigDto dto) {
        return service.create(dto);
    }
}
