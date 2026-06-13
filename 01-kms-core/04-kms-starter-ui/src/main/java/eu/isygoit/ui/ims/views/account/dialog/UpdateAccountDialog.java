package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UpdateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final Long accountId;
    private final Runnable onSuccess;

    private TextField tenantField;
    private EmailField emailField;
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    private TextField accountTypeField;

    // Image related
    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile newImageFile;
    private Button changeImageButton;
    private boolean imageChanged = false;

    private AccountDto currentAccount;

    public UpdateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               Long accountId,
                               Runnable onSuccess) {
        super("Edit Account");
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.accountId = accountId;
        this.onSuccess = onSuccess;

        setOkButtonText("Save");
        setWidth("90%");
        getElement().getStyle().set("max-width", "700px");

        buildForm();
        add(buildLayout());
        loadAccountData();
    }

    private void buildForm() {
        tenantField = new TextField("Tenant *");
        tenantField.setRequiredIndicatorVisible(true);

        emailField = new EmailField("Email *");
        emailField.setRequiredIndicatorVisible(true);

        firstNameField = new TextField("First name");
        lastNameField = new TextField("Last name");
        phoneNumberField = new TextField("Phone number");
        languageCombo = new ComboBox<>("Language");
        languageCombo.setItems(IEnumLanguage.Types.values());
        functionRoleField = new TextField("Function role");
        isAdminCheckbox = new Checkbox("Is administrator");
        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        accountTypeField = new TextField("Account type");

        // Image thumbnail + change button
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

        changeImageButton = new Button("Change Image", e -> openCropperDialog());
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
            ResponseEntity<Resource> response = accountImageService.downloadImage(accountId);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] imageBytes = toByteArray(response.getBody());
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
        form.add(tenantField, emailField, firstNameField, lastNameField,
                phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo, accountTypeField);
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

    private void loadAccountData() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = accountService.findById(accountId);
            if (response.getBody() != null) {
                currentAccount = response.getBody();
                populateFields();
                loadExistingImage();
            } else {
                append("Account not found");
                close();
            }
        } catch (FeignException ex) {
            append("Failed to load account: " + extractErrorMessage(ex));
            close();
        } catch (Exception e) {
            append("Failed to load account: " + e.getMessage());
            close();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void populateFields() {
        tenantField.setValue(currentAccount.getTenant() != null ? currentAccount.getTenant() : "");
        emailField.setValue(currentAccount.getEmail() != null ? currentAccount.getEmail() : "");
        if (currentAccount.getAccountDetails() != null) {
            firstNameField.setValue(currentAccount.getAccountDetails().getFirstName() != null ? currentAccount.getAccountDetails().getFirstName() : "");
            lastNameField.setValue(currentAccount.getAccountDetails().getLastName() != null ? currentAccount.getAccountDetails().getLastName() : "");
        }
        phoneNumberField.setValue(currentAccount.getPhoneNumber() != null ? currentAccount.getPhoneNumber() : "");
        languageCombo.setValue(currentAccount.getLanguage());
        functionRoleField.setValue(currentAccount.getFunctionRole() != null ? currentAccount.getFunctionRole() : "");
        isAdminCheckbox.setValue(Boolean.TRUE.equals(currentAccount.getIsAdmin()));
        adminStatusCombo.setValue(currentAccount.getAdminStatus());
        accountTypeField.setValue(currentAccount.getAccountType() != null ? currentAccount.getAccountType() : "");
    }

    @Override
    protected boolean onOk() {
        if (tenantField.getValue().isBlank()) {
            append("Tenant is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }

        parentView.showLoading(true);
        try {
            currentAccount.setTenant(tenantField.getValue());
            currentAccount.setEmail(emailField.getValue());
            currentAccount.setPhoneNumber(phoneNumberField.getValue());
            currentAccount.setLanguage(languageCombo.getValue());
            currentAccount.setFunctionRole(functionRoleField.getValue());
            currentAccount.setIsAdmin(isAdminCheckbox.getValue());
            currentAccount.setAdminStatus(adminStatusCombo.getValue());
            currentAccount.setAccountType(accountTypeField.getValue());

            if (currentAccount.getAccountDetails() == null) {
                currentAccount.setAccountDetails(new AccountDetailsDto());
            }
            currentAccount.getAccountDetails().setFirstName(firstNameField.getValue());
            currentAccount.getAccountDetails().setLastName(lastNameField.getValue());

            ResponseEntity<AccountDto> response = accountService.update(accountId, currentAccount);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append("Update failed: HTTP " + response.getStatusCodeValue());
                return false;
            }

            if (imageChanged && newImageFile != null) {
                ResponseEntity<AccountDto> uploadResponse = accountImageService.uploadImage(accountId, newImageFile);
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append("Account updated but image upload failed: HTTP " + uploadResponse.getStatusCodeValue());
                    return false;
                }
            }

            append("Account updated successfully");
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