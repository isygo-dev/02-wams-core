package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.ProfilePhotoSection;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the customer create/update dialogs: every editable
 * {@link CustomerDto} field is declared here once, in three short tabs
 * (identity, contact, address). {@code id} is never shown; {@code accountCode}
 * is managed by the link-account dialog. Subclasses only decide how the DTO is
 * persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractCustomerFormDialog extends ImsActionDialog {

    final CustomerManagementView parentView;
    final CustomerService customerService;
    final CustomerImageService customerImageService;
    private final String messagePrefix;

    /* identity */
    TextField nameField;
    TextArea descriptionField;
    ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    /* contact */
    EmailField emailField;
    TextField phoneField;
    TextField urlField;
    TextField tenantField;

    /* address */
    TextField countryField;
    TextField stateField;
    TextField cityField;
    TextField streetField;
    TextField zipCodeField;
    TextField additionalInfoField;

    ProfilePhotoSection photoSection;

    AbstractCustomerFormDialog(String title,
                               Runnable onSuccess,
                               String messagePrefix,
                               CustomerManagementView parentView,
                               CustomerService customerService,
                               CustomerImageService customerImageService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerImageService = customerImageService;
    }

    /* Hooks */

    /** I18n key of the "upload / change image" button. */
    abstract String photoButtonKey();

    /** Whether a newly cropped image is mandatory to save. */
    abstract boolean imageRequired();

    /** The DTO that receives the form values on save. */
    abstract CustomerDto target();

    /** Persists the DTO; returns the stored customer id, or null after appending an error. */
    abstract Long persist(CustomerDto dto);

    /* Layout */

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        photoSection = new ProfilePhotoSection(
                I18n.t("ims.customer.dialog.field.image"),
                I18n.t("ims.customer.dialog.field.image.help"),
                I18n.t(photoButtonKey()),
                I18n.t("ims.customer.dialog.field.remove.image"));

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add(new Tab(I18n.t("ims.customer.dialog.tab.identity")), buildIdentityTab());
        tabs.add(new Tab(I18n.t("ims.customer.dialog.tab.contact")), buildContactTab());
        tabs.add(new Tab(I18n.t("ims.customer.dialog.tab.address")), buildAddressTab());

        VerticalLayout root = DialogLayout.stack();
        root.add(photoSection, tabs);
        addContent(root);
    }

    private Component buildIdentityTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.customer.details.section.identity"), VaadinIcon.USER_CARD);
        FormLayout form = DialogLayout.responsiveForm();

        nameField = new TextField(I18n.t("ims.customer.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setClearButtonVisible(true);

        adminStatusCombo = new ComboBox<>(I18n.t("ims.customer.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        adminStatusCombo.setRenderer(ImsEnumTag.renderer(null));

        descriptionField = DialogLayout.tall(new TextArea(I18n.t("ims.customer.dialog.field.description")));

        form.add(nameField, adminStatusCombo);
        form.add(descriptionField, 2);

        section.add(form);
        return section;
    }

    private Component buildContactTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.customer.details.section.contact"), VaadinIcon.PHONE);
        FormLayout form = DialogLayout.responsiveForm();

        emailField = new EmailField(I18n.t("ims.customer.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);

        phoneField = new TextField(I18n.t("ims.customer.dialog.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);

        urlField = new TextField(I18n.t("ims.customer.dialog.field.website"));
        tenantField = new TextField(I18n.t("ims.customer.dialog.field.tenant"));

        form.add(emailField, phoneField);
        form.add(urlField, tenantField);

        section.add(form);
        return section;
    }

    private Component buildAddressTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.customer.details.section.address"), VaadinIcon.MAP_MARKER);
        FormLayout form = DialogLayout.responsiveForm();

        countryField = new TextField(I18n.t("ims.customer.dialog.field.country"));
        stateField = new TextField(I18n.t("ims.customer.dialog.field.state"));
        cityField = new TextField(I18n.t("ims.customer.dialog.field.city"));
        streetField = new TextField(I18n.t("ims.customer.dialog.field.street"));
        zipCodeField = new TextField(I18n.t("ims.customer.dialog.field.zip.code"));
        additionalInfoField = new TextField(I18n.t("ims.customer.dialog.field.additional.info"));

        form.add(countryField, stateField);
        form.add(cityField, streetField);
        form.add(zipCodeField, additionalInfoField);

        section.add(form);
        return section;
    }

    /* DTO <-> form */

    /** Fills every field from the DTO (update flow). */
    final void fillFrom(CustomerDto dto) {
        nameField.setValue(nvl(dto.getName()));
        descriptionField.setValue(nvl(dto.getDescription()));
        adminStatusCombo.setValue(dto.getAdminStatus());

        emailField.setValue(nvl(dto.getEmail()));
        phoneField.setValue(nvl(dto.getPhoneNumber()));
        urlField.setValue(nvl(dto.getUrl()));
        tenantField.setValue(nvl(dto.getTenant()));

        AddressDto addr = dto.getAddress();
        if (addr != null) {
            countryField.setValue(nvl(addr.getCountry()));
            stateField.setValue(nvl(addr.getState()));
            cityField.setValue(nvl(addr.getCity()));
            streetField.setValue(nvl(addr.getStreet()));
            zipCodeField.setValue(nvl(addr.getZipCode()));
            additionalInfoField.setValue(nvl(addr.getAdditionalInfo()));
        }
    }

    /** Copies every field into the DTO. {@code id} and {@code accountCode} are left untouched. */
    private void applyTo(CustomerDto dto) {
        dto.setName(nameField.getValue());
        dto.setEmail(emailField.getValue());
        dto.setPhoneNumber(phoneField.getValue());
        dto.setUrl(urlField.getValue());
        dto.setDescription(descriptionField.getValue());
        dto.setAdminStatus(adminStatusCombo.getValue());
        if (!tenantField.getValue().isBlank()) {
            dto.setTenant(tenantField.getValue().trim());
        }
        dto.setAddress(hasAddressData() ? buildAddress() : null);
    }

    private AddressDto buildAddress() {
        return AddressDto.builder()
                .country(countryField.getValue())
                .state(stateField.getValue())
                .city(cityField.getValue())
                .street(streetField.getValue())
                .zipCode(zipCodeField.getValue())
                .additionalInfo(additionalInfoField.getValue())
                .build();
    }

    private boolean hasAddressData() {
        return !countryField.getValue().isBlank()
                || !stateField.getValue().isBlank()
                || !cityField.getValue().isBlank()
                || !streetField.getValue().isBlank()
                || !zipCodeField.getValue().isBlank();
    }

    private boolean isValid() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.customer.dialog.field.name.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.customer.dialog.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.customer.dialog.field.phone.required"));
            return false;
        }
        if (imageRequired() && photoSection.getSelectedFile() == null) {
            append(I18n.t("ims.customer.dialog.create.image.required"));
            return false;
        }
        return true;
    }

    /* Save */

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            CustomerDto dto = target();
            applyTo(dto);

            Long customerId = persist(dto);
            if (customerId == null) {
                return false;
            }

            if (photoSection.getSelectedFile() != null) {
                ResponseEntity<CustomerDto> uploadResponse =
                        customerImageService.uploadImage(customerId, photoSection.getSelectedFile());
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t(messagePrefix + ".image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(CustomerDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
