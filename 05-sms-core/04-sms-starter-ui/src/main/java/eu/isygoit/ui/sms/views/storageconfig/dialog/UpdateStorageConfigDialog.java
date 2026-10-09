package eu.isygoit.ui.sms.views.storageconfig.dialog;

import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link StorageConfigDto}. The form itself lives in
 * {@link AbstractStorageConfigFormDialog}; this class only updates the configuration
 * (an empty password keeps the current one).
 */
public class UpdateStorageConfigDialog extends AbstractStorageConfigFormDialog {

    private final StorageConfigDto config;

    public UpdateStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     StorageConfigDto config,
                                     Runnable onSuccess) {
        super(I18n.t("sms.storageconfig.dialog.update.title"), onSuccess,
                "sms.storageconfig.dialog.update", false, parentView, storageConfigService);
        this.config = config;
        setOkButtonText(I18n.t("sms.storageconfig.dialog.update.button"));

        buildForm();
        fillFrom(config);
    }

    @Override
    StorageConfigDto target() {
        return config;
    }

    @Override
    ResponseEntity<StorageConfigDto> persist(StorageConfigDto dto) {
        return service.update(dto.getId(), dto);
    }
}
