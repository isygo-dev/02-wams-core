package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UpdateApplicationDialog extends BaseActionDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final ApplicationImageService applicationImageService;
    private final ApplicationDto application;
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
    private MultipartFile newImageFile;
    private Button changeImageButton;
    private boolean imageChanged = false;

    public UpdateApplicationDialog(ApplicationManagementView parentView,
                                   ApplicationService applicationService,
                                   ApplicationImageService applicationImageService,
                                   ApplicationDto application,
                                   Runnable onSuccess) {
        super(I18n.t("ims.app.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationImageService = applicationImageService;
        this.application = application;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.app.dialog.update.button"));
        setWidth("700px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
        populateFields();
        loadExistingImage();
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("ims.app.dialog.update.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        titleField = new TextField(I18n.t("ims.app.dialog.update.field.title"));
        titleField.setRequiredIndicatorVisible(true);
        titleField.setWidthFull();

        codeField = new TextField(I18n.t("ims.app.dialog.update.field.code"));
        codeField.setWidthFull();

        categoryField = new TextField(I18n.t("ims.app.dialog.update.field.category"));
        categoryField.setWidthFull();

        urlField = new TextField(I18n.t("ims.app.dialog.update.field.url"));
        urlField.setRequiredIndicatorVisible(true);
        urlField.setWidthFull();

        orderField = new IntegerField(I18n.t("ims.app.dialog.update.field.order"));
        orderField.setWidthFull();

        descriptionField = new TextArea(I18n.t("ims.app.dialog.update.field.description"));
        descriptionField.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.app.dialog.update.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setWidthFull();

        imageThumbnail = new Image();
        imageThumbnail.setWidth("60px");
        imageThumbnail.setHeight("60px");
        imageThumbnail.setVisible(false);
        imageThumbnail.addClassName("wams-image-thumbnail");

        imagePlaceholder = new Div();
        imagePlaceholder.setWidth("60px");
        imagePlaceholder.setHeight("60px");
        imagePlaceholder.addClassName("wams-image-placeholder");
        imagePlaceholder.add(new Icon(VaadinIcon.CAMERA));

        changeImageButton = new Button(I18n.t("ims.app.dialog.field.change.image"), e -> openCropperDialog());
        changeImageButton.setIcon(new Icon(VaadinIcon.UPLOAD));
    }

    private void openCropperDialog() {
        ImageCropperDialog cropperDialog = new ImageCropperDialog(croppedImage -> {
            if (croppedImage != null) {
                newImageFile = croppedImage;
                imageChanged = true;
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

    private void loadExistingImage() {
        try {
            ResponseEntity<Resource> response = applicationImageService.downloadImage(application.getId());
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Resource resource = response.getBody();
                byte[] imageBytes = toByteArray(resource);
                String base64 = "data:image/jpeg;base64," + java.util.Base64.getEncoder().encodeToString(imageBytes);
                imageThumbnail.setSrc(base64);
                imageThumbnail.setVisible(true);
                imagePlaceholder.setVisible(false);
            }
        } catch (Exception e) {
            // No existing image or error – keep placeholder
        }
    }

    private byte[] toByteArray(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
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
        HorizontalLayout imageRow = new HorizontalLayout(imagePlaceholder, imageThumbnail, changeImageButton);
        imageRow.setAlignItems(FlexComponent.Alignment.CENTER);
        imageRow.setSpacing(true);
        return imageRow;
    }

    private Div buildLayout() {
        Div main = new Div();
        main.add(buildFormLayout(), buildImageRow());
        return main;
    }

    private void populateFields() {
        nameField.setValue(application.getName() != null ? application.getName() : "");
        titleField.setValue(application.getTitle() != null ? application.getTitle() : "");
        codeField.setValue(application.getCode() != null ? application.getCode() : "");
        categoryField.setValue(application.getCategory() != null ? application.getCategory() : "");
        urlField.setValue(application.getUrl() != null ? application.getUrl() : "");
        orderField.setValue(application.getOrder());
        descriptionField.setValue(application.getDescription() != null ? application.getDescription() : "");
        adminStatusCombo.setValue(application.getAdminStatus());
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.update.field.name.required"));
            return false;
        }
        if (titleField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.update.field.title.required"));
            return false;
        }
        if (urlField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.update.field.url.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            application.setName(nameField.getValue());
            application.setTitle(titleField.getValue());
            application.setCode(codeField.getValue());
            application.setCategory(categoryField.getValue());
            application.setUrl(urlField.getValue());
            application.setOrder(orderField.getValue());
            application.setDescription(descriptionField.getValue());
            application.setAdminStatus(adminStatusCombo.getValue());

            ResponseEntity<ApplicationDto> response = applicationService.update(application.getId(), application);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.app.dialog.update.failed", (response.getBody() != null ? response.getBody() : I18n.t("ims.app.dialog.update.no.response.body"))));
                return false;
            }

            if (imageChanged && newImageFile != null) {
                ResponseEntity<ApplicationDto> uploadResponse = applicationImageService.uploadImage(application.getId(), newImageFile);
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t("ims.app.dialog.update.image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t("ims.app.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.app.dialog.update.error", e.getMessage()));
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