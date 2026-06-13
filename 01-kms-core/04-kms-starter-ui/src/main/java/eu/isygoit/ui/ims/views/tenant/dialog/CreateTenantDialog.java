package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class CreateTenantDialog extends BaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final TenantImageService tenantImageService;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField codeField;
    private EmailField emailField;
    private TextField phoneField;
    private TextField industryField;
    private TextField urlField;
    private TextArea descriptionField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile selectedImageFile;
    private Button uploadImageButton;

    public CreateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              TenantImageService tenantImageService,
                              Runnable onSuccess) {
        super("Create Tenant");
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantImageService = tenantImageService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("90%");
        getElement().getStyle().set("max-width", "700px");

        buildForm();
        add(buildLayout());
    }

    private void buildForm() {
        nameField = new TextField("Name *");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder("Acme Corporation");

        codeField = new TextField("Code (unique)");
        codeField.setPlaceholder("acme-corp");

        emailField = new EmailField("Email *");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder("contact@acme.com");

        phoneField = new TextField("Phone *");
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setPlaceholder("+1 234 567 8900");

        industryField = new TextField("Industry");
        industryField.setPlaceholder("Technology");

        urlField = new TextField("Website URL");
        urlField.setPlaceholder("https://acme.com");

        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Brief description of the tenant");

        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);

        // Image thumbnail + upload button
        imageThumbnail = new Image();
        imageThumbnail.setWidth("60px");
        imageThumbnail.setHeight("60px");
        imageThumbnail.setVisible(false);
        imageThumbnail.getStyle().set("border-radius", "50%").set("object-fit", "cover");

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

        uploadImageButton = new Button("Upload Image", e -> openCropperDialog());
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

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );
        form.add(nameField, codeField, emailField, phoneField,
                industryField, urlField, descriptionField, adminStatusCombo);
        form.setColspan(descriptionField, 2);
        return form;
    }

    private HorizontalLayout buildImageRow() {
        HorizontalLayout imageRow = new HorizontalLayout(imagePlaceholder, imageThumbnail, uploadImageButton);
        imageRow.setAlignItems(FlexComponent.Alignment.CENTER);
        imageRow.setSpacing(true);
        return imageRow;
    }

    private Div buildLayout() {
        Div main = new Div();
        main.add(buildFormLayout(), buildImageRow());
        return main;
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append("Name is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append("Phone is required");
            return false;
        }
        if (selectedImageFile == null) {
            append("Please upload and crop an image for the tenant");
            return false;
        }

        parentView.showLoading(true);
        try {
            TenantDto newTenant = TenantDto.builder()
                    .name(nameField.getValue())
                    .code(codeField.getValue())
                    .email(emailField.getValue())
                    .phone(phoneField.getValue())
                    .industry(industryField.getValue())
                    .url(urlField.getValue())
                    .description(descriptionField.getValue())
                    .adminStatus(adminStatusCombo.getValue())
                    .build();

            ResponseEntity<TenantDto> createResponse = tenantService.create(newTenant);
            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                append("Tenant creation failed: HTTP " + createResponse.getStatusCodeValue());
                return false;
            }

            Long tenantId = createResponse.getBody().getId();
            if (tenantId == null) {
                append("Created tenant has no ID");
                return false;
            }

            ResponseEntity<TenantDto> uploadResponse = tenantImageService.uploadImage(tenantId, selectedImageFile);
            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                append("Tenant created but image upload failed: HTTP " + uploadResponse.getStatusCodeValue());
                return false;
            }

            append("Tenant created successfully with image");
            if (onSuccess != null) onSuccess.run();
            return true;

        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {}
        return ex.getMessage();
    }
}