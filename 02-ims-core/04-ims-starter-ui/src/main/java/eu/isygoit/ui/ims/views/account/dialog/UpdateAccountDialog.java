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
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UpdateAccountDialog extends BaseActionDialog {

    private final AccountManagementView parentView;
    private final AccountService accountService;
    private final AccountImageService accountImageService;
    private final TenantService tenantService;
    private final Long accountId;
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

    // Image related
    private Image imageThumbnail;
    private Div imagePlaceholder;
    private MultipartFile newImageFile;
    private Button changeImageButton;
    private boolean imageChanged = false;

    private AccountDto currentAccount;
    private List<TenantDto> tenants = new ArrayList<>();

    public UpdateAccountDialog(AccountManagementView parentView,
                               AccountService accountService,
                               AccountImageService accountImageService,
                               TenantService tenantService,
                               Long accountId,
                               Runnable onSuccess) {
        super(I18n.t("ims.account.dialog.update.title"), onSuccess);
        this.parentView = parentView;
        this.accountService = accountService;
        this.accountImageService = accountImageService;
        this.tenantService = tenantService;
        this.accountId = accountId;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.account.dialog.update.button"));
        setWidth("700px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildLayout());
        loadTenants();
        loadAccountData();
    }

    private void buildForm() {
        tenantCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.tenant"));
        tenantCombo.setReadOnly(true);
        tenantCombo.setRequiredIndicatorVisible(true);
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
        accountTypeCombo.setWidthFull();

        emailField = new EmailField(I18n.t("ims.account.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setWidthFull();

        firstNameField = new TextField(I18n.t("ims.account.dialog.field.first.name"));
        firstNameField.setWidthFull();

        lastNameField = new TextField(I18n.t("ims.account.dialog.field.last.name"));
        lastNameField.setWidthFull();

        phoneNumberField = new TextField(I18n.t("ims.account.dialog.field.phone"));
        phoneNumberField.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setWidthFull();

        functionRoleField = new TextField(I18n.t("ims.account.dialog.field.function.role"));
        functionRoleField.setWidthFull();

        isAdminCheckbox = new Checkbox(I18n.t("ims.account.dialog.field.is.admin"));
        isAdminCheckbox.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setWidthFull();

        // Image thumbnail + change button
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

        changeImageButton = new Button(I18n.t("ims.account.dialog.field.change.image"), e -> openCropperDialog());
        changeImageButton.setIcon(new Icon(VaadinIcon.UPLOAD));
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
        return tenants.stream().filter(t -> t.getCode().equals(code)).findFirst().orElse(null);
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
            // No existing image – keep placeholder
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
        form.add(tenantCombo, accountTypeCombo, emailField, firstNameField,
                lastNameField, phoneNumberField, languageCombo, functionRoleField,
                isAdminCheckbox, adminStatusCombo);
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
                append(I18n.t("ims.account.dialog.update.not.found"));
                close();
            }
        } catch (FeignException ex) {
            append(I18n.t("ims.account.dialog.update.load.error", extractErrorMessage(ex)));
            close();
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.update.load.error", e.getMessage()));
            close();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void populateFields() {
        tenantCombo.setValue(currentAccount.getTenant());
        accountTypeCombo.setValue(currentAccount.getAccountType());
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

        parentView.showLoading(true);
        try {
            currentAccount.setTenant(tenantCombo.getValue());
            currentAccount.setAccountType(accountTypeCombo.getValue());
            currentAccount.setEmail(emailField.getValue());
            currentAccount.setPhoneNumber(phoneNumberField.getValue());
            currentAccount.setLanguage(languageCombo.getValue());
            currentAccount.setFunctionRole(functionRoleField.getValue());
            currentAccount.setIsAdmin(isAdminCheckbox.getValue());
            currentAccount.setAdminStatus(adminStatusCombo.getValue());

            if (currentAccount.getAccountDetails() == null) {
                currentAccount.setAccountDetails(new AccountDetailsDto());
            }
            currentAccount.getAccountDetails().setFirstName(firstNameField.getValue());
            currentAccount.getAccountDetails().setLastName(lastNameField.getValue());

            ResponseEntity<AccountDto> response = accountService.update(accountId, currentAccount);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.account.dialog.update.failed", response.getStatusCodeValue()));
                return false;
            }

            if (imageChanged && newImageFile != null) {
                ResponseEntity<AccountDto> uploadResponse = accountImageService.uploadImage(accountId, newImageFile);
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t("ims.account.dialog.update.image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t("ims.account.dialog.update.success"));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.account.dialog.update.failed", e.getMessage()));
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