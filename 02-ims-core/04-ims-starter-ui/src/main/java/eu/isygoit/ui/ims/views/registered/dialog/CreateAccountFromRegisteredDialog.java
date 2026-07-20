package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CreateAccountFromRegisteredDialog extends BaseActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final RegisteredUserDto registeredUser;
    private final Runnable onSuccess;

    // New tenant fields (TenantInfo)
    private TextField tenantNameField;
    private EmailField tenantEmailField;
    private TextField tenantPhoneField;
    private TextField tenantIndustryField;
    private TextField tenantUrlField;
    private TextArea tenantDescriptionField;

    // Account fields (AccountInfo)
    private ComboBox<String> accountTypeCombo;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    public CreateAccountFromRegisteredDialog(RegisteredManagementView parentView,
                                             RegisteredUserService registeredUserService,
                                             RegisteredUserDto registeredUser,
                                             Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.create.account.title"), onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUser = registeredUser;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.registered.dialog.create.account.button"));
        setWidth("780px");
        setMaxWidth("95%");
        addClassName("wams-create-account-dialog");

        buildForm();
        addContent(buildLayout());
    }

    private void buildForm() {
        // Tenant fields
        tenantNameField = new TextField(I18n.t("ims.tenant.dialog.field.name"));
        tenantNameField.setRequiredIndicatorVisible(true);
        tenantNameField.setPlaceholder(I18n.t("ims.tenant.dialog.field.name.placeholder"));
        tenantNameField.setWidthFull();
        tenantNameField.setValue(registeredUser.getOrganisation() != null ? registeredUser.getOrganisation() : "");
        tenantNameField.setReadOnly(true);
        tenantNameField.addClassName("wams-readonly-field");

        tenantEmailField = new EmailField(I18n.t("ims.tenant.dialog.field.email"));
        tenantEmailField.setRequiredIndicatorVisible(true);
        tenantEmailField.setPlaceholder(I18n.t("ims.tenant.dialog.field.email.placeholder"));
        tenantEmailField.setWidthFull();
        tenantEmailField.setValue(registeredUser.getEmail() != null ? registeredUser.getEmail() : "");

        tenantPhoneField = new TextField(I18n.t("ims.tenant.dialog.field.phone"));
        tenantPhoneField.setRequiredIndicatorVisible(true);
        tenantPhoneField.setPlaceholder(I18n.t("ims.tenant.dialog.field.phone.placeholder"));
        tenantPhoneField.setWidthFull();
        tenantPhoneField.setValue(registeredUser.getPhoneNumber() != null ? registeredUser.getPhoneNumber() : "");

        tenantIndustryField = new TextField(I18n.t("ims.tenant.dialog.field.industry"));
        tenantIndustryField.setPlaceholder(I18n.t("ims.tenant.dialog.field.industry.placeholder"));
        tenantIndustryField.setWidthFull();

        tenantUrlField = new TextField(I18n.t("ims.tenant.dialog.field.website"));
        tenantUrlField.setPlaceholder(I18n.t("ims.tenant.dialog.field.website.placeholder"));
        tenantUrlField.setWidthFull();

        tenantDescriptionField = new TextArea(I18n.t("ims.tenant.dialog.field.description"));
        tenantDescriptionField.setPlaceholder(I18n.t("ims.tenant.dialog.field.description.placeholder"));
        tenantDescriptionField.setWidthFull();

        // Account fields
        accountTypeCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.account.type"));
        accountTypeCombo.setRequiredIndicatorVisible(true);
        accountTypeCombo.setItems(
                AccountTypeConstants.SUPER_ADMIN,
                AccountTypeConstants.TENANT_ADMIN,
                AccountTypeConstants.TENANT_USER
        );
        accountTypeCombo.setValue(AccountTypeConstants.TENANT_USER);
        accountTypeCombo.setWidthFull();

        languageCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setValue(IEnumLanguage.Types.EN);
        languageCombo.setWidthFull();

        functionRoleField = new TextField(I18n.t("ims.account.dialog.field.function.role"));
        functionRoleField.setRequiredIndicatorVisible(true);
        functionRoleField.setPlaceholder(I18n.t("ims.account.dialog.field.function.role.placeholder"));
        functionRoleField.setWidthFull();
        functionRoleField.setValue(registeredUser.getFunctionRole() != null ? registeredUser.getFunctionRole() : "");

        isAdminCheckbox = new Checkbox(I18n.t("ims.account.dialog.field.is.admin"));
        isAdminCheckbox.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);
        adminStatusCombo.setWidthFull();
    }

    private Div buildUserInfo() {
        Div userInfo = new Div();
        userInfo.addClassName("wams-user-info-card");

        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        Icon userIcon = VaadinIcon.USER.create();
        userIcon.setSize("40px");
        userIcon.addClassName("wams-user-avatar");

        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);

        String fullName = (registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "") +
                " " + (registeredUser.getLastName() != null ? registeredUser.getLastName() : "");
        H4 nameLabel = new H4(fullName.trim());
        nameLabel.getStyle().set("margin", "0");
        nameLabel.addClassName(LumoUtility.FontSize.MEDIUM);

        Span emailLabel = new Span(registeredUser.getEmail());
        emailLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        emailLabel.addClassName(LumoUtility.FontSize.SMALL);

        Span organisationLabel = new Span(
                I18n.t("ims.registered.card.organisation") + ": " +
                        (registeredUser.getOrganisation() != null ? registeredUser.getOrganisation() : "—")
        );
        organisationLabel.addClassName(LumoUtility.TextColor.TERTIARY);
        organisationLabel.addClassName(LumoUtility.FontSize.XSMALL);

        info.add(nameLabel, emailLabel, organisationLabel);
        layout.add(userIcon, info);
        userInfo.add(layout);

        return userInfo;
    }

    private Div buildLayout() {
        Div main = new Div();

        // User info
        main.add(buildUserInfo());

        // Tenant section
        Div tenantSection = new Div();
        tenantSection.addClassName("wams-tenant-section");

        H3 tenantTitle = new H3(I18n.t("ims.registered.dialog.create.account.tenant.section"));
        tenantTitle.addClassName(LumoUtility.FontSize.MEDIUM);
        tenantTitle.addClassName(LumoUtility.Margin.Bottom.XSMALL);
        tenantSection.add(tenantTitle);

        FormLayout tenantForm = new FormLayout();
        tenantForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        tenantForm.add(
                tenantNameField,
                tenantEmailField,
                tenantPhoneField,
                tenantIndustryField,
                tenantUrlField
        );
        tenantForm.setColspan(tenantDescriptionField, 2);
        tenantForm.add(tenantDescriptionField);

        tenantSection.add(tenantForm);
        main.add(tenantSection);

        // Account section
        Div accountSection = new Div();
        accountSection.addClassName("wams-account-section");

        H3 accountTitle = new H3(I18n.t("ims.registered.dialog.create.account.account.section"));
        accountTitle.addClassName(LumoUtility.FontSize.MEDIUM);
        accountTitle.addClassName(LumoUtility.Margin.Bottom.XSMALL);
        accountSection.add(accountTitle);

        FormLayout accountForm = new FormLayout();
        accountForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        accountForm.add(
                accountTypeCombo,
                languageCombo,
                functionRoleField,
                isAdminCheckbox,
                adminStatusCombo
        );

        accountSection.add(accountForm);
        main.add(accountSection);

        return main;
    }

    @Override
    protected boolean onOk() {
        // Validate tenant fields
        if (tenantNameField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.name.required"));
            return false;
        }
        if (tenantEmailField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.email.required"));
            return false;
        }
        if (tenantPhoneField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.phone.required"));
            return false;
        }

        // Validate account fields
        if (functionRoleField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.function.role.required"));
            return false;
        }

        // Build TenantInfo
        CreateAccountFromRegisteredRequestDto.TenantInfo tenantInfo =
                CreateAccountFromRegisteredRequestDto.TenantInfo.builder()
                        .industry(tenantIndustryField.getValue())
                        .url(tenantUrlField.getValue())
                        .description(tenantDescriptionField.getValue())
                        .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                        .build();

        // Build AccountInfo
        CreateAccountFromRegisteredRequestDto.AccountInfo accountInfo =
                CreateAccountFromRegisteredRequestDto.AccountInfo.builder()
                        .accountType(accountTypeCombo.getValue())
                        .language(languageCombo.getValue())
                        .functionalRole(functionRoleField.getValue())
                        .isAdmin(isAdminCheckbox.getValue())
                        .adminStatus(adminStatusCombo.getValue())
                        .accountDetails(
                                eu.isygoit.dto.data.AccountDetailsDto.builder()
                                        .firstName(registeredUser.getFirstName())
                                        .lastName(registeredUser.getLastName())
                                        .build()
                        )
                        .build();

        // Build request DTO
        CreateAccountFromRegisteredRequestDto request =
                CreateAccountFromRegisteredRequestDto.builder()
                        .email(registeredUser.getEmail())
                        .tenantInfo(tenantInfo)
                        .accountInfo(accountInfo)
                        .build();

        parentView.showLoading(true);
        try {
            // Call the unified API
            ResponseEntity<AccountDto> response = registeredUserService.createAccountFromRegistered(
                    request
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                append(I18n.t("ims.registered.dialog.create.account.failed",
                        response.getStatusCodeValue()));
                return false;
            }

            append(I18n.t("ims.registered.dialog.create.account.success"));

            Notification.show(
                    I18n.t("ims.registered.dialog.create.account.notification", registeredUser.getEmail()),
                    5000,
                    Notification.Position.BOTTOM_END
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onSuccess != null) onSuccess.run();
            return true;

        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.create.account.error", e.getMessage()));
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