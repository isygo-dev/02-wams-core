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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateAccountFromRegisteredDialog extends BaseActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final AccountService accountService;
    private final TenantService tenantService;
    private final RegisteredUserDto registeredUser;
    private final Runnable onSuccess;

    // Link existing tenant checkbox
    private Checkbox linkExistingTenantCheckbox;

    // Existing tenant fields
    private ComboBox<String> tenantCombo;
    private List<TenantDto> tenants = new ArrayList<>();

    // New tenant fields
    private TextField tenantNameField;
    private TextField tenantCodeField;
    private EmailField tenantEmailField;
    private TextField tenantPhoneField;
    private TextField tenantIndustryField;
    private TextField tenantUrlField;
    private TextArea tenantDescriptionField;

    // Account fields
    private ComboBox<String> accountTypeCombo;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    private PasswordField passwordField;

    private Div existingTenantContainer;
    private Div newTenantContainer;

    public CreateAccountFromRegisteredDialog(RegisteredManagementView parentView,
                                             RegisteredUserService registeredUserService,
                                             AccountService accountService,
                                             TenantService tenantService,
                                             RegisteredUserDto registeredUser,
                                             Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.create.account.title"), onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.registeredUser = registeredUser;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("ims.registered.dialog.create.account.button"));
        setWidth("780px");
        setMaxWidth("95%");
        addClassName("wams-create-account-dialog");

        buildForm();
        addContent(buildLayout());
        loadTenants();
    }

    private void buildForm() {
        // Link existing tenant checkbox
        linkExistingTenantCheckbox = new Checkbox(I18n.t("ims.registered.dialog.create.account.link.existing"));
        linkExistingTenantCheckbox.setValue(true);
        linkExistingTenantCheckbox.addClassName("wams-toggle-checkbox");
        linkExistingTenantCheckbox.addValueChangeListener(e -> toggleTenantMode());

        // Existing tenant selection
        tenantCombo = new ComboBox<>();
        tenantCombo.setLabel(I18n.t("ims.registered.dialog.create.account.tenant"));
        tenantCombo.setRequiredIndicatorVisible(true);
        tenantCombo.setPlaceholder(I18n.t("ims.registered.dialog.create.account.tenant.placeholder"));
        tenantCombo.setItemLabelGenerator(item -> {
            TenantDto tenant = findTenantByCode(item);
            return tenant != null ? tenant.getName() + " (" + tenant.getCode() + ")" : item;
        });
        tenantCombo.setAllowCustomValue(false);
        tenantCombo.setWidthFull();

        // New tenant fields - compact layout
        tenantNameField = new TextField(I18n.t("ims.tenant.dialog.field.name"));
        tenantNameField.setRequiredIndicatorVisible(false);
        tenantNameField.setPlaceholder(I18n.t("ims.tenant.dialog.field.name.placeholder"));
        tenantNameField.setWidthFull();

        tenantCodeField = new TextField(I18n.t("ims.tenant.dialog.field.code"));
        tenantCodeField.setPlaceholder(I18n.t("ims.tenant.dialog.field.code.placeholder"));
        tenantCodeField.setWidthFull();

        tenantEmailField = new EmailField(I18n.t("ims.tenant.dialog.field.email"));
        tenantEmailField.setRequiredIndicatorVisible(false);
        tenantEmailField.setPlaceholder(I18n.t("ims.tenant.dialog.field.email.placeholder"));
        tenantEmailField.setWidthFull();

        tenantPhoneField = new TextField(I18n.t("ims.tenant.dialog.field.phone"));
        tenantPhoneField.setRequiredIndicatorVisible(false);
        tenantPhoneField.setPlaceholder(I18n.t("ims.tenant.dialog.field.phone.placeholder"));
        tenantPhoneField.setWidthFull();

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

        // Containers
        existingTenantContainer = new Div();
        existingTenantContainer.addClassName("wams-tenant-container");
        existingTenantContainer.add(tenantCombo);

        newTenantContainer = new Div();
        newTenantContainer.addClassName("wams-tenant-container");
        newTenantContainer.setVisible(false);
    }

    private void loadTenants() {
        parentView.showLoading(true);
        try {
            ResponseEntity<List<TenantDto>> response = tenantService.findAllList();
            if (response.getBody() != null) {
                tenants = response.getBody();
                tenantCombo.setItems(tenants.stream().map(TenantDto::getName).collect(Collectors.toList()));
            }
        } catch (FeignException ex) {
            append(I18n.t("ims.registered.dialog.create.account.load.tenants.error", extractErrorMessage(ex)));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.create.account.load.tenants.error", e.getMessage()));
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

    private void toggleTenantMode() {
        boolean isLinkExisting = linkExistingTenantCheckbox.getValue();
        existingTenantContainer.setVisible(isLinkExisting);
        newTenantContainer.setVisible(!isLinkExisting);

        tenantCombo.setRequiredIndicatorVisible(isLinkExisting);
        tenantNameField.setRequiredIndicatorVisible(!isLinkExisting);
        tenantEmailField.setRequiredIndicatorVisible(!isLinkExisting);
        tenantPhoneField.setRequiredIndicatorVisible(!isLinkExisting);
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

        info.add(nameLabel, emailLabel);
        layout.add(userIcon, info);
        userInfo.add(layout);

        return userInfo;
    }

    private Div buildToggleSection() {
        Div section = new Div();
        section.addClassName("wams-toggle-section");

        HorizontalLayout toggleLayout = new HorizontalLayout();
        toggleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        toggleLayout.setSpacing(true);
        toggleLayout.setWidthFull();

        Icon buildingIcon = VaadinIcon.BUILDING_O.create();
        buildingIcon.setSize("18px");
        buildingIcon.addClassName("wams-toggle-icon");

        Span toggleLabel = new Span(I18n.t("ims.registered.dialog.create.account.link.existing.description"));
        toggleLabel.addClassName(LumoUtility.FontSize.SMALL);
        toggleLabel.addClassName(LumoUtility.TextColor.SECONDARY);

        toggleLayout.add(buildingIcon, linkExistingTenantCheckbox, toggleLabel);
        toggleLayout.expand(toggleLabel);

        section.add(toggleLayout);
        return section;
    }

    private Div buildLayout() {
        Div main = new Div();

        // User info
        main.add(buildUserInfo());

        // Tenant mode toggle
        main.add(buildToggleSection());

        // Tenant selection/creation
        Div tenantSection = new Div();
        tenantSection.addClassName("wams-tenant-section");

        // Existing tenant
        existingTenantContainer.addClassName("wams-tenant-container--active");
        tenantSection.add(existingTenantContainer);

        // New tenant form - compact grid
        FormLayout newTenantForm = new FormLayout();
        newTenantForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );
        newTenantForm.add(
                tenantNameField,
                tenantCodeField,
                tenantEmailField,
                tenantPhoneField,
                tenantIndustryField,
                tenantUrlField
        );
        newTenantForm.setColspan(tenantDescriptionField, 2);
        newTenantForm.add(tenantDescriptionField);

        newTenantContainer.add(newTenantForm);
        tenantSection.add(newTenantContainer);

        main.add(tenantSection);

        // Account section - compact
        Div accountSection = new Div();
        accountSection.addClassName("wams-account-section");

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
        accountForm.setColspan(passwordField, 2);
        accountForm.add(passwordField);

        accountSection.add(accountForm);
        main.add(accountSection);

        return main;
    }

    @Override
    protected boolean onOk() {
        boolean isLinkExisting = linkExistingTenantCheckbox.getValue();

        String tenantName;
        if (isLinkExisting) {
            if (tenantCombo.getValue() == null || tenantCombo.getValue().isBlank()) {
                append(I18n.t("ims.registered.dialog.create.account.tenant.required"));
                return false;
            }
            tenantName = tenantCombo.getValue();
        } else {
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
            tenantName = null;
        }

        if (passwordField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.password.required"));
            return false;
        }

        parentView.showLoading(true);
        try {
            if (!isLinkExisting) {
                TenantDto newTenant = TenantDto.builder()
                        .name(tenantNameField.getValue())
                        .code(tenantCodeField.getValue().isBlank() ? null : tenantCodeField.getValue())
                        .email(tenantEmailField.getValue())
                        .phone(tenantPhoneField.getValue())
                        .industry(tenantIndustryField.getValue())
                        .url(tenantUrlField.getValue())
                        .description(tenantDescriptionField.getValue())
                        .adminStatus(IEnumEnabledBinaryStatus.Types.ENABLED)
                        .build();

                ResponseEntity<TenantDto> createTenantResponse = tenantService.create(newTenant);
                if (!createTenantResponse.getStatusCode().is2xxSuccessful() || createTenantResponse.getBody() == null) {
                    append(I18n.t("ims.registered.dialog.create.account.tenant.create.failed",
                            createTenantResponse.getStatusCodeValue()));
                    return false;
                }
                tenantName = createTenantResponse.getBody().getName();
                append(I18n.t("ims.registered.dialog.create.account.tenant.created",
                        createTenantResponse.getBody().getName()));
            }

            AccountDto newAccount = new AccountDto();
            newAccount.setTenant(tenantName);
            newAccount.setAccountType(accountTypeCombo.getValue());
            newAccount.setEmail(registeredUser.getEmail());
            newAccount.setPhoneNumber(registeredUser.getPhoneNumber());
            newAccount.setLanguage(languageCombo.getValue());
            newAccount.setFunctionRole(functionRoleField.getValue());
            newAccount.setIsAdmin(isAdminCheckbox.getValue());
            newAccount.setAdminStatus(adminStatusCombo.getValue());
            newAccount.setRoleInfo(new ArrayList<>());

            eu.isygoit.dto.data.AccountDetailsDto details = new eu.isygoit.dto.data.AccountDetailsDto();
            details.setFirstName(registeredUser.getFirstName());
            details.setLastName(registeredUser.getLastName());
            newAccount.setAccountDetails(details);

            ResponseEntity<AccountDto> createAccountResponse = accountService.create(newAccount);
            if (!createAccountResponse.getStatusCode().is2xxSuccessful() || createAccountResponse.getBody() == null) {
                append(I18n.t("ims.registered.dialog.create.account.failed",
                        createAccountResponse.getStatusCodeValue()));
                return false;
            }

            registeredUserService.update(registeredUser.getId(), registeredUser);

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