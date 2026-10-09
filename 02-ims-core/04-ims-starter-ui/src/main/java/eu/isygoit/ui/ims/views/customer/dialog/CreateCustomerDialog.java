package eu.isygoit.ui.ims.views.customer.dialog;

import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Create dialog for {@link CustomerDto}. The form itself lives in
 * {@link AbstractCustomerFormDialog}; this class only creates the customer.
 */
public class CreateCustomerDialog extends AbstractCustomerFormDialog {

    public CreateCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                CustomerImageService customerImageService,
                                Runnable onSuccess) {
        super(I18n.t("ims.customer.dialog.create.title"), onSuccess, "ims.customer.dialog.create",
                parentView, customerService, customerImageService);
        setOkButtonText(I18n.t("ims.customer.dialog.create.button"));

        buildForm();
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
    }

    @Override
    String photoButtonKey() {
        return "ims.customer.dialog.field.upload.image";
    }

    @Override
    boolean imageRequired() {
        return true;
    }

    @Override
    CustomerDto target() {
        return CustomerDto.builder().build();
    }

    @Override
    Long persist(CustomerDto dto) {
        ResponseEntity<CustomerDto> response = customerService.create(dto);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.customer.dialog.create.failed", response.getStatusCodeValue()));
            return null;
        }
        Long customerId = response.getBody().getId();
        if (customerId == null) {
            append(I18n.t("ims.customer.dialog.create.no.id"));
            return null;
        }
        return customerId;
    }
}
