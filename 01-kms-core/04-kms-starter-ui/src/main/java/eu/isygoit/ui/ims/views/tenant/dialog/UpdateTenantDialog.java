package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
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
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UpdateTenantDialog extends BaseActionDialog {

    private final TenantManagementView parentView;
    private final TenantService tenantService;
    private final TenantImageService tenantImageService;
    private final TenantDto tenant;
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
    private MultipartFile newImageFile;
    private Button changeImageButton;
    private boolean imageChanged = false;

    public UpdateTenantDialog(TenantManagementView parentView,
                              TenantService tenantService,
                              TenantImageService tenantImageService,
                              TenantDto tenant,
                              Runnable onSuccess) {
        super(I18n.t("ims.tenant.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantImageService = tenantImageService;
        this.tenant = tenant;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.tenant.dialog.update.button"));
        setWidth("700px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
        populateFields();
        loadExistingImage();
    }

    private void buildForm() {
        nameField = new TextField(I18n.t("ims.tenant.dialog.update.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setWidthFull();

        codeField = new TextField(I18n.t("ims.tenant.dialog.update.field.code"));
        codeField.setWidthFull();

        emailField = new EmailField(I18n.t("ims.tenant.dialog.update.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        phoneField = new TextField(I18n.t("ims.tenant.dialog.update.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setWidthFull();

        industryField = new TextField(I18n.t("ims.tenant.dialog.update.field.industry"));
        industryField.setWidthFull();

        urlField = new TextField(I18n.t("ims.tenant.dialog.update.field.website"));
        urlField.setWidthFull();

        descriptionField = new TextArea(I18n.t("ims.tenant.dialog.update.field.description"));
        descriptionField.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.tenant.dialog.update.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
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

        changeImageButton = new Button(I18n.t("ims.tenant.dialog.field.change.image"), e -> openCropperDialog());
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
            ResponseEntity<Resource> response = tenantImageService.downloadImage(tenant.getId());
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
        form.add(nameField, codeField, emailField, phoneField,
                industryField, urlField, descriptionField, adminStatusCombo);
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
        nameField.setValue(tenant.getName() != null ? tenant.getName() : "");
        codeField.setValue(tenant.getCode() != null ? tenant.getCode() : "");
        emailField.setValue(tenant.getEmail() != null ? tenant.getEmail() : "");
        phoneField.setValue(tenant.getPhone() != null ? tenant.getPhone() : "");
        industryField.setValue(tenant.getIndustry() != null ? tenant.getIndustry() : "");
        urlField.setValue(tenant.getUrl() != null ? tenant.getUrl() : "");
        descriptionField.setValue(tenant.getDescription() != null ? tenant.getDescription() : "");
        adminStatusCombo.setValue(tenant.getAdminStatus());
    }

    @Override
    protected boolean onOk() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.update.field.name.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.update.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.update.field.phone.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            tenant.setName(nameField.getValue());
            tenant.setCode(codeField.getValue());
            tenant.setEmail(emailField.getValue());
            tenant.setPhone(phoneField.getValue());
            tenant.setIndustry(industryField.getValue());
            tenant.setUrl(urlField.getValue());
            tenant.setDescription(descriptionField.getValue());
            tenant.setAdminStatus(adminStatusCombo.getValue());

            ResponseEntity<TenantDto> response = tenantService.update(tenant.getId(), tenant);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.tenant.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            if (imageChanged && newImageFile != null) {
                ResponseEntity<TenantDto> uploadResponse = tenantImageService.uploadImage(tenant.getId(), newImageFile);
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t("ims.tenant.dialog.update.image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t("ims.tenant.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.tenant.dialog.update.error", e.getMessage()));
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