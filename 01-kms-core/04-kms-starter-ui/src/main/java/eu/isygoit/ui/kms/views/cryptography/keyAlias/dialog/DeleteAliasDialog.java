package eu.isygoit.ui.kms.views.cryptography.keyAlias.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import eu.isygoit.dto.KmsDtos.DeleteAliasResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;
import eu.isygoit.ui.kms.views.cryptography.keyAlias.AliasesView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Dialog for deleting an alias.
 * If the alias is the primary key, a 9‑digit confirmation code is required.
 * Otherwise, it behaves like a simple confirmation dialog.
 */
public class DeleteAliasDialog extends PinBaseActionDialog {

    private final AliasesView parentView;
    private final KmsApiService kmsApiService;
    private final String aliasName;

    public DeleteAliasDialog(AliasesView parentView,
                             KmsApiService kmsApiService,
                             Runnable onSuccess,
                             String aliasName,
                             Boolean primaryKey) {
        super(I18n.t("alias.dialog.delete.title"),
                primaryKey ? I18n.t("alias.dialog.delete.primary.warning")
                        : I18n.t("alias.dialog.delete.confirmation", aliasName),
                onSuccess,
                primaryKey); // only require PIN for primary key
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.aliasName = aliasName;

        setOkButtonText(I18n.t("alias.dialog.delete.button"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_ERROR);
        setWidth("500px");
    }

    @Override
    protected boolean onOk() {
        // Extra safety: validate PIN again (the base class already validated the button, but double-check)
        if (!validatePin()) {
            append(I18n.t("alias.dialog.delete.invalid.code"));
            parentView.showLoading(false);
            return false;
        }

        parentView.showLoading(true);
        try {
            ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("alias.dialog.delete.failed", response.getStatusCode()));
                return false;
            }

            append(I18n.t("alias.dialog.delete.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("alias.dialog.delete.failed.operation", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }

        return false;
    }
}