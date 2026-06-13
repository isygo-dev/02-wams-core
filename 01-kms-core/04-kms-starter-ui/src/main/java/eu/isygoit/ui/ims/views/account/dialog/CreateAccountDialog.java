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
import com.vaadin.flow.component.textfield.PasswordField;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;

public class CreateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
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
    private PasswordField passwordField;

    // Image related
    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile selectedImageFile;
    private Button uploadImageButton;

    public CreateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               Runnable onSuccess) {
        super("Create Account");
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.onSuccess = onSuccess;

        setOkButtonText("Create");
        setWidth("90%");
        getElement().getStyle().set("max-width", "700px");

        buildForm();
        add(buildLayout());
    }

    private void buildForm() {
        tenantField = new TextField("Tenant *");
        tenantField.setRequiredIndicatorVisible(true);
        tenantField.setPlaceholder("acme-corp");

        emailField = new EmailField("Email *");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder("user@example.com");

        firstNameField = new TextField("First name");
        firstNameField.setPlaceholder("John");

        lastNameField = new TextField("Last name");
        lastNameField.setPlaceholder("Doe");

        phoneNumberField = new TextField("Phone number");
        phoneNumberField.setPlaceholder("+1 234 567 8900");

        languageCombo = new ComboBox<>("Language");
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setValue(IEnumLanguage.Types.EN);

        functionRoleField = new TextField("Function role");
        functionRoleField.setPlaceholder("e.g., ADMIN, USER");

        isAdminCheckbox = new Checkbox("Is administrator");

        adminStatusCombo = new ComboBox<>("Admin status");
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);

        accountTypeField = new TextField("Account type");
        accountTypeField.setValue("TENANT_USER");
        accountTypeField.setReadOnly(true);

        passwordField = new PasswordField("Initial password *");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setPlaceholder("••••••••");

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
        form.add(tenantField, emailField, firstNameField, lastNameField,
                phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo, accountTypeField, passwordField);
        // Make password field span full width on larger screens for better UX
        form.setColspan(passwordField, 2);
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
        if (tenantField.getValue().isBlank()) {
            append("Tenant is required");
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append("Email is required");
            return false;
        }
        if (passwordField.getValue().isBlank()) {
            append("Initial password is required");
            return false;
        }
        if (selectedImageFile == null) {
            append("Please upload and crop an image for the account");
            return false;
        }

        parentView.showLoading(true);
        try {
            AccountDto newAccount = new AccountDto();
            newAccount.setTenant(tenantField.getValue());
            newAccount.setEmail(emailField.getValue());
            newAccount.setPhoneNumber(phoneNumberField.getValue());
            newAccount.setLanguage(languageCombo.getValue());
            newAccount.setFunctionRole(functionRoleField.getValue());
            newAccount.setIsAdmin(isAdminCheckbox.getValue());
            newAccount.setAdminStatus(adminStatusCombo.getValue());
            newAccount.setAccountType(accountTypeField.getValue());

            AccountDetailsDto details = new AccountDetailsDto();
            details.setFirstName(firstNameField.getValue());
            details.setLastName(lastNameField.getValue());
            newAccount.setAccountDetails(details);
            newAccount.setRoleInfo(new ArrayList<>());

            // Note: Password is not part of AccountDto. The backend might require a separate DTO.
            // Assuming the service endpoint used for creation also accepts password via AccountDto
            // or we need to call a different method. If not, adjust accordingly.
            ResponseEntity<AccountDto> createResponse = accountService.create(newAccount);
            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                append("Account creation failed: HTTP " + createResponse.getStatusCodeValue());
                return false;
            }

            Long accountId = createResponse.getBody().getId();
            if (accountId == null) {
                append("Created account has no ID");
                return false;
            }

            // Upload image
            ResponseEntity<AccountDto> uploadResponse = accountImageService.uploadImage(accountId, selectedImageFile);
            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                append("Account created but image upload failed: HTTP " + uploadResponse.getStatusCodeValue());
                return false;
            }

            append("Account created successfully with image");
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