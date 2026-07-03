package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;

public class DeleteStorageConfigDialog extends PinBaseActionDialog {

    private final StorageConfigManagementView parentView;
    private final StorageConfigService storageConfigService;
    private final Long configId;

    public DeleteStorageConfigDialog(StorageConfigManagementView parentView,
                                     StorageConfigService storageConfigService,
                                     Long configId,
                                     Runnable onSuccess) {
        super(I18n.t("sms.storageconfig.dialog.delete.title"),
                I18n.t("sms.storageconfig.dialog.delete.message"),
                onSuccess);
        this.parentView = parentView;
        this.storageConfigService = storageConfigService;
        this.configId = configId;

        setOkButtonText(I18n.t("sms.storageconfig.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("450px");
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(I18n.t("sms.storageconfig.dialog.delete.invalid.code"));
            return false;
        }

        parentView.showLoading(true);
        try {
            storageConfigService.delete(configId);
            append(I18n.t("sms.storageconfig.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.storageconfig.dialog.delete.error", e.getMessage()));
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