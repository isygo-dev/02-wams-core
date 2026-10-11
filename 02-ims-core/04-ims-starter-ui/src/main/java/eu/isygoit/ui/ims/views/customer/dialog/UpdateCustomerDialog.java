package eu.isygoit.ui.ims.views.customer.dialog;

import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import org.springframework.http.ResponseEntity;

/**
 * Update dialog for {@link CustomerDto}. The form itself lives in
 * {@link AbstractCustomerFormDialog}; this class only loads and updates the customer.
 *
 * <p>The existing profile photo is downloaded by the abstract form when
 * {@code fillFrom(...)} runs.
 */
public class UpdateCustomerDialog extends AbstractCustomerFormDialog {

    private final CustomerDto customer;

    public UpdateCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                CustomerImageService customerImageService,
                                CustomerDto customer,
                                Runnable onSuccess) {
        super(I18n.t("ims.customer.dialog.update.title"), onSuccess, "ims.customer.dialog.update",
                parentView, customerService, customerImageService);
        this.customer = customer;
        setOkButtonText(I18n.t("ims.customer.dialog.update.button"));

        buildForm();
        fillFrom(customer);
    }

    @Override
    String photoButtonKey() {
        return "ims.customer.dialog.field.change.image";
    }

    @Override
    boolean imageRequired() {
        return false;
    }

    @Override
    CustomerDto target() {
        return customer;
    }

    @Override
    Long persist(CustomerDto dto) {
        ResponseEntity<CustomerDto> response;
        try {
            response = customerService.update(dto.getId(), dto);
        } catch (Exception e) {
            append(I18n.t("ims.customer.dialog.update.failed", "—"));
            return null;
        }
        if (response == null || !response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            append(I18n.t("ims.customer.dialog.update.failed",
                    response == null ? "no response" : response.getStatusCodeValue()));
            return null;
        }
        return dto.getId();
    }
}