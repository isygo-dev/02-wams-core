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
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import eu.isygoit.ui.ims.views.account.AccountManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final TenantService tenantService;
    private final Runnable onSuccess;

    private ComboBox<String> tenantCombo;
    private ComboBox<String> accountTypeCombo;
    private EmailField emailField;
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField phoneNumberField;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    private PasswordField passwordField;

    // Image related
    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile selectedImageFile;
    private Button uploadImageButton;

    private List<TenantDto> tenants = new ArrayList<>();

    public CreateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               TenantService tenantService,
                               Runnable onSuccess) {
        super(I18n.t("ims.account.dialog.create.title"), onSuccess);
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.tenantService = tenantService;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.account.dialog.create.button"));
        setWidth("700px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
        loadTenants();
    }

    private void buildForm() {
        // Tenant selection
        tenantCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.tenant"));
        tenantCombo.setRequiredIndicatorVisible(true);
        tenantCombo.setPlaceholder(I18n.t("ims.account.dialog.field.tenant.placeholder"));
        tenantCombo.setItemLabelGenerator(item -> {
            TenantDto tenant = findTenantByCode(item);
            return tenant != null ? tenant.getName() + " (" + tenant.getCode() + ")" : item;
        });
        tenantCombo.setAllowCustomValue(false);
        tenantCombo.setWidthFull();

        accountTypeCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.account.type"));
        accountTypeCombo.setRequiredIndicatorVisible(true);
        accountTypeCombo.setItems(
                AccountTypeConstants.SUPER_ADMIN,
                AccountTypeConstants.TENANT_ADMIN,
                AccountTypeConstants.TENANT_USER
        );
        accountTypeCombo.setAllowCustomValue(true);
        accountTypeCombo.setPlaceholder(I18n.t("ims.account.dialog.field.account.type.placeholder"));
        accountTypeCombo.setValue(AccountTypeConstants.TENANT_USER);
        accountTypeCombo.setWidthFull();

        emailField = new EmailField(I18n.t("ims.account.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder(I18n.t("ims.account.dialog.field.email.placeholder"));
        emailField.setWidthFull();

        firstNameField = new TextField(I18n.t("ims.account.dialog.field.first.name"));
        firstNameField.setPlaceholder(I18n.t("ims.account.dialog.field.first.name.placeholder"));
        firstNameField.setWidthFull();

        lastNameField = new TextField(I18n.t("ims.account.dialog.field.last.name"));
        lastNameField.setPlaceholder(I18n.t("ims.account.dialog.field.last.name.placeholder"));
        lastNameField.setWidthFull();

        phoneNumberField = new TextField(I18n.t("ims.account.dialog.field.phone"));
        phoneNumberField.setPlaceholder(I18n.t("ims.account.dialog.field.phone.placeholder"));
        phoneNumberField.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setValue(IEnumLanguage.Types.EN);
        languageCombo.setWidthFull();

        functionRoleField = new TextField(I18n.t("ims.account.dialog.field.function.role"));
        functionRoleField.setPlaceholder(I18n.t("ims.account.dialog.field.function.role.placeholder"));
        functionRoleField.setWidthFull();

        isAdminCheckbox = new Checkbox(I18n.t("ims.account.dialog.field.is.admin"));
        isAdminCheckbox.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
        adminStatusCombo.setWidthFull();

        passwordField = new PasswordField(I18n.t("ims.account.dialog.field.password"));
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setPlaceholder(I18n.t("ims.account.dialog.field.password.placeholder"));
        passwordField.setWidthFull();

        // Image thumbnail + upload button
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

        uploadImageButton = new Button(I18n.t("ims.account.dialog.field.upload.image"), e -> openCropperDialog());
        uploadImageButton.setIcon(new Icon(VaadinIcon.UPLOAD));
    }

    private void loadTenants() {
        parentView.showLoading(true);
        try {
            ResponseEntity<List<TenantDto>> response = tenantService.findAllList();
            if (response.getBody() != null && response.getBody() != null) {
                tenants = response.getBody();
                tenantCombo.setItems(tenants.stream().map(TenantDto::getCode).collect(Collectors.toList()));
            }
        } catch (FeignException ex) {
            append(I18n.t("ims.account.dialog.load.tenants.error", extractErrorMessage(ex)));
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.load.tenants.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
    }

    private TenantDto findTenantByCode(String code) {
        return tenants.stream()
                .filter(t -> t.getCode().equals(code))
                .findFirst()
                .orElse(null);
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
        form.add(tenantCombo, accountTypeCombo, emailField, firstNameField,
                lastNameField, phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo, passwordField);
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
        if (tenantCombo.getValue() == null || tenantCombo.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.tenant.required"));
            return false;
        }
        if (accountTypeCombo.getValue() == null || accountTypeCombo.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.account.type.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.email.required"));
            return false;
        }
        if (passwordField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.password.required"));
            return false;
        }
        if (selectedImageFile == null) {
            append(I18n.t("ims.account.dialog.create.image.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            AccountDto newAccount = new AccountDto();
            newAccount.setTenant(tenantCombo.getValue());
            newAccount.setAccountType(accountTypeCombo.getValue());
            newAccount.setEmail(emailField.getValue());
            newAccount.setPhoneNumber(phoneNumberField.getValue());
            newAccount.setLanguage(languageCombo.getValue());
            newAccount.setFunctionRole(functionRoleField.getValue());
            newAccount.setIsAdmin(isAdminCheckbox.getValue());
            newAccount.setAdminStatus(adminStatusCombo.getValue());

            AccountDetailsDto details = new AccountDetailsDto();
            details.setFirstName(firstNameField.getValue());
            details.setLastName(lastNameField.getValue());
            newAccount.setAccountDetails(details);
            newAccount.setRoleInfo(new ArrayList<>());

            ResponseEntity<AccountDto> createResponse = accountService.create(newAccount);
            if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
                append(I18n.t("ims.account.dialog.create.failed", createResponse.getStatusCodeValue()));
                return false;
            }

            Long accountId = createResponse.getBody().getId();
            if (accountId == null) {
                append(I18n.t("ims.account.dialog.create.no.id"));
                return false;
            }

            ResponseEntity<AccountDto> uploadResponse = accountImageService.uploadImage(accountId, selectedImageFile);
            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                append(I18n.t("ims.account.dialog.create.image.failed", uploadResponse.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.account.dialog.create.success"));
            if (onSuccess != null) onSuccess.run();
            return true;

        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.create.failed", e.getMessage()));
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