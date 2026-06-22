package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public class CreateApplicationDialog extends BaseActionDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final ApplicationImageService applicationImageService;
    private final Runnable onSuccess;

    private TextField nameField;
    private TextField titleField;
    private TextField codeField;
    private TextField categoryField;
    private TextField urlField;
    private IntegerField orderField;
    private TextArea descriptionField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile selectedImageFile;
    private Button uploadImageButton;

    public CreateApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   ApplicationImageService applicationImageService,
                                   Runnable onSuccess) {
        super("Create Application", onSuccess);
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationImageService = applicationImageService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("700px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
    }

    private void buildForm() {
        nameField = new TextField("Name *");
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder("Acme App");
        nameField.setWidthFull();

        titleField = new TextField("Title *");
        titleField.setRequiredIndicatorVisible(true);
        titleField.setPlaceholder("Acme Application");
        titleField.setWidthFull();

        codeField = new TextField("Code (unique)");
        codeField.setPlaceholder("acme-app");
        codeField.setWidthFull();

        categoryField = new TextField("Category");
        categoryField.setValue("PRM Store");
        categoryField.setPlaceholder("PRM Store");
        categoryField.setWidthFull();

        urlField = new TextField("URL *");
        urlField.setRequiredIndicatorVisible(true);
        urlField.setPlaceholder("https://acme.com/app");
        urlField.setWidthFull();

        orderField = new IntegerField("Display Order");
        orderField.setPlaceholder("0");
        orderField.setWidthFull();

        descriptionField = new TextArea("Description");
        descriptionField.setPlaceholder("Brief description of the application");
        descriptionField.setWidthFull();

        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
        adminStatusCombo.setWidthFull();

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
        form.add(nameField, titleField, codeField, categoryField,
                urlField, orderField, descriptionField, adminStatusCombo);
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
        if (titleField.getValue().isBlank()) {
            append("Title is required");
            return false;
        }
        if (urlField.getValue().isBlank()) {
            append("URL is required");
            return false;
        }
        if (selectedImageFile == null) {
            append("Please upload and crop an image for the application");
            return false;
        }

        parentView.showLoading(true);
        try {
            ApplicationDto newApp = ApplicationDto.builder()
                    .name(nameField.getValue())
                    .title(titleField.getValue())
                    .code(codeField.getValue())
                    .category(categoryField.getValue())
                    .url(urlField.getValue())
                    .order(orderField.getValue())
                    .description(descriptionField.getValue())
                    .adminStatus(adminStatusCombo.getValue())
                    .build();

            ResponseEntity<ApplicationDto> createResponse = applicationService.create(newApp);
            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                append("Application creation failed: HTTP " + createResponse.getStatusCodeValue());
                return false;
            }

            Long appId = createResponse.getBody().getId();
            if (appId == null) {
                append("Created application has no ID");
                return false;
            }

            ResponseEntity<ApplicationDto> uploadResponse = applicationImageService.uploadImage(appId, selectedImageFile);
            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                append("Application created but image upload failed: HTTP " + uploadResponse.getStatusCodeValue());
                return false;
            }

            append("Application created successfully with image");
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
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}