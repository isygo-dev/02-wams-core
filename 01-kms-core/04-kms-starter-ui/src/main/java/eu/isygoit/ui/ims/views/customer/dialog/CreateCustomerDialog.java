package eu.isygoit.ui.ims.views.customer.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.customer.CustomerManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class CreateCustomerDialog extends BaseActionDialog {

    private final CustomerManagementView parentView;
    private final CustomerService customerService;
    private final CustomerImageService customerImageService;
    private final Runnable onSuccess;

    private TextField nameField;
    private EmailField emailField;
    private TextField phoneField;
    private TextField urlField;
    private TextArea descriptionField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    // Address fields
    private TextField countryField;
    private TextField stateField;
    private TextField cityField;
    private TextField streetField;
    private TextField zipCodeField;
    private TextField additionalInfoField;

    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile selectedImageFile;
    private Button uploadImageButton;

    public CreateCustomerDialog(CustomerManagementView parentView,
                                CustomerService customerService,
                                CustomerImageService customerImageService,
                                Runnable onSuccess) {
        super(I18n.t("customer.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.customerService = customerService;
        this.customerImageService = customerImageService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("customer.dialog.create.button"));
        setWidth("800px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("customer.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        emailField = new EmailField(I18n.t("customer.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        phoneField = new TextField(I18n.t("customer.dialog.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setWidthFull();

        urlField = new TextField(I18n.t("customer.dialog.field.website"));
        urlField.setWidthFull();

        descriptionField = new TextArea(I18n.t("customer.dialog.field.description"));
        descriptionField.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("customer.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
        adminStatusCombo.setWidthFull();

        // Address
        countryField = new TextField(I18n.t("customer.dialog.field.country"));
        countryField.setWidthFull();

        stateField = new TextField(I18n.t("customer.dialog.field.state"));
        stateField.setWidthFull();

        cityField = new TextField(I18n.t("customer.dialog.field.city"));
        cityField.setWidthFull();

        streetField = new TextField(I18n.t("customer.dialog.field.street"));
        streetField.setWidthFull();

        zipCodeField = new TextField(I18n.t("customer.dialog.field.zip.code"));
        zipCodeField.setWidthFull();

        additionalInfoField = new TextField(I18n.t("customer.dialog.field.additional.info"));
        additionalInfoField.setWidthFull();

        // Image
        imageThumbnail = new Image();
        imageThumbnail.setWidth("60px");
        imageThumbnail.setHeight("60px");
        imageThumbnail.setVisible(false);
        imageThumbnail.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("border", "2px solid var(--lumo-contrast-20pct)");

        imagePlaceholder = new Div();
        imagePlaceholder.setWidth("60px");
        imagePlaceholder.setHeight("60px");
        imagePlaceholder.getStyle()
                .set("background", "#f0f0f0")
                .set("border-radius", "50%")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");
        imagePlaceholder.add(new Icon(VaadinIcon.CAMERA));

        uploadImageButton = new Button(I18n.t("customer.dialog.field.upload.image"), e -> openCropperDialog());
        uploadImageButton.setIcon(new Icon(VaadinIcon.UPLOAD));
    }

    private void openCropperDialog() {
        ImageCropperDialog cropperDialog = new ImageCropperDialog(croppedImage -> {
            if (croppedImage != null) {
                selectedImageFile = croppedImage;
                updateImageThumbnail(croppedImage);
            }
        });
        cropperDialog.open();
    }

    private void updateImageThumbnail(MultipartFile imageFile) {
        try {
            String base64 = "data:" + imageFile.getContentType() + ";base64," +
                    java.util.Base64.getEncoder().encodeToString(imageFile.getBytes());
            imageThumbnail.setSrc(base64);
            imageThumbnail.setVisible(true);
            imagePlaceholder.setVisible(false);
        } catch (Exception e) {
            // ignore
        }
    }

    private FormLayout buildMainForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.add(nameField, emailField, phoneField, urlField, descriptionField, adminStatusCombo);
        form.setColspan(descriptionField, 2);
        return form;
    }

    private FormLayout buildAddressForm() {
        FormLayout addressForm = new FormLayout();
        addressForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        addressForm.add(countryField, stateField, cityField, streetField, zipCodeField, additionalInfoField);
        return addressForm;
    }

    private HorizontalLayout buildImageRow() {
        HorizontalLayout imageRow = new HorizontalLayout(imagePlaceholder, imageThumbnail, uploadImageButton);
        imageRow.setAlignItems(FlexComponent.Alignment.CENTER);
        imageRow.setSpacing(true);
        return imageRow;
    }

    private Div buildLayout() {
        Div main = new Div();
        main.add(buildMainForm(), new H4(I18n.t("customer.dialog.field.address")), buildAddressForm(), buildImageRow());
        return main;
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("customer.dialog.field.name.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("customer.dialog.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("customer.dialog.field.phone.required"));
            return false;
        }
        if (selectedImageFile == null) {
            append(I18n.t("customer.dialog.create.image.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            AddressDto address = null;
            if (hasAddressData()) {
                address = AddressDto.builder()
                        .country(countryField.getValue())
                        .state(stateField.getValue())
                        .city(cityField.getValue())
                        .street(streetField.getValue())
                        .zipCode(zipCodeField.getValue())
                        .additionalInfo(additionalInfoField.getValue())
                        .build();
            }

            CustomerDto newCustomer = CustomerDto.builder()
                    .name(nameField.getValue())
                    .email(emailField.getValue())
                    .phoneNumber(phoneField.getValue())
                    .url(urlField.getValue())
                    .description(descriptionField.getValue())
                    .adminStatus(adminStatusCombo.getValue())
                    .address(address)
                    .build();

            ResponseEntity<CustomerDto> createResponse = customerService.create(newCustomer);
            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                append(I18n.t("customer.dialog.create.failed", createResponse.getStatusCodeValue()));
                return false;
            }

            Long customerId = createResponse.getBody().getId();
            if (customerId == null) {
                append(I18n.t("customer.dialog.create.no.id"));
                return false;
            }

            ResponseEntity<CustomerDto> uploadResponse = customerImageService.uploadImage(customerId, selectedImageFile);
            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("customer.dialog.create.image.failed", uploadResponse.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("customer.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;

        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("customer.dialog.create.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private boolean hasAddressData() {
        return (countryField.getValue() != null && !countryField.getValue().isBlank()) ||
                (stateField.getValue() != null && !stateField.getValue().isBlank()) ||
                (cityField.getValue() != null && !cityField.getValue().isBlank()) ||
                (streetField.getValue() != null && !streetField.getValue().isBlank()) ||
                (zipCodeField.getValue() != null && !zipCodeField.getValue().isBlank());
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