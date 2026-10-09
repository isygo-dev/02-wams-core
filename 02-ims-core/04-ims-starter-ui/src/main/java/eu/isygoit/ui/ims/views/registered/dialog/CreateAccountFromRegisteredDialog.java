package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.constants.AccountTypeConstants;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.request.CreateAccountFromRegisteredRequestDto;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.DetailHero;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates an account (and its tenant) from a registered user. The form is aligned
 * on {@link CreateAccountFromRegisteredRequestDto}: tenant information and account
 * information (with account details) are edited in two tabs; the registered user is
 * only summarised in the header (name, e-mail, organisation and phone are taken
 * from it by the server, they are not request fields).
 */
public class CreateAccountFromRegisteredDialog extends ImsActionDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final RegisteredUserDto registeredUser;

    // Tenant information (TenantInfo)
    private TextField tenantIndustryField;
    private TextField tenantUrlField;
    private TextField tenantAddressField;
    private ComboBox<IEnumEnabledBinaryStatus.Types> tenantAdminStatusCombo;
    private TextArea tenantDescriptionField;

    // Account information (AccountInfo)
    private ComboBox<String> accountTypeCombo;
    private ComboBox<IEnumLanguage.Types> languageCombo;
    private TextField functionRoleField;
    private Checkbox isAdminCheckbox;
    private ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;

    // Account details (AccountDetailsDto)
    private TextField firstNameField;
    private TextField lastNameField;
    private TextField countryField;

    public CreateAccountFromRegisteredDialog(RegisteredManagementView parentView,
                                             RegisteredUserService registeredUserService,
                                             RegisteredUserDto registeredUser,
                                             Runnable onSuccess) {
        super(I18n.t("ims.registered.dialog.create.account.title"), onSuccess);
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUser = registeredUser;

        setOkButtonText(I18n.t("ims.registered.dialog.create.account.button"));
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        buildForm();
        addContent(buildLayout());
    }

    private void buildForm() {
        // Tenant information
        tenantIndustryField = new TextField(I18n.t("ims.tenant.dialog.field.industry"));
        tenantIndustryField.setPlaceholder(I18n.t("ims.tenant.dialog.field.industry.placeholder"));

        tenantUrlField = new TextField(I18n.t("ims.tenant.dialog.field.website"));
        tenantUrlField.setPlaceholder(I18n.t("ims.tenant.dialog.field.website.placeholder"));

        tenantAddressField = new TextField(I18n.t("ims.registered.dialog.create.account.tenant.address"));

        tenantAdminStatusCombo = new ComboBox<>(I18n.t("ims.registered.dialog.create.account.tenant.admin.status"));
        tenantAdminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        tenantAdminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        tenantAdminStatusCombo.setRenderer(ImsEnumTag.renderer(null));
        tenantAdminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);

        tenantDescriptionField = DialogLayout.tall(new TextArea(I18n.t("ims.tenant.dialog.field.description")));
        tenantDescriptionField.setPlaceholder(I18n.t("ims.tenant.dialog.field.description.placeholder"));

        // Account information
        accountTypeCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.account.type"));
        accountTypeCombo.setRequiredIndicatorVisible(true);
        accountTypeCombo.setItems(
                AccountTypeConstants.SUPER_ADMIN,
                AccountTypeConstants.TENANT_ADMIN,
                AccountTypeConstants.TENANT_USER
        );
        accountTypeCombo.setValue(AccountTypeConstants.TENANT_USER);

        languageCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.language"));
        languageCombo.setItems(IEnumLanguage.Types.values());
        languageCombo.setItemLabelGenerator(language -> ImsEnumTag.label(language, "ims.enum.language"));
        languageCombo.setRenderer(ImsEnumTag.renderer("ims.enum.language"));
        languageCombo.setValue(IEnumLanguage.Types.EN);

        functionRoleField = new TextField(I18n.t("ims.account.dialog.field.function.role"));
        functionRoleField.setRequiredIndicatorVisible(true);
        functionRoleField.setPlaceholder(I18n.t("ims.account.dialog.field.function.role.placeholder"));
        functionRoleField.setValue(registeredUser.getFunctionRole() != null ? registeredUser.getFunctionRole() : "");

        isAdminCheckbox = new Checkbox(I18n.t("ims.account.dialog.field.is.admin"));

        adminStatusCombo = new ComboBox<>(I18n.t("ims.account.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        adminStatusCombo.setRenderer(ImsEnumTag.renderer(null));
        adminStatusCombo.setValue(IEnumEnabledBinaryStatus.Types.ENABLED);

        // Account details (pre-filled from the registered user, as the server would)
        firstNameField = new TextField(I18n.t("ims.registered.dialog.create.account.first.name"));
        firstNameField.setValue(registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "");

        lastNameField = new TextField(I18n.t("ims.registered.dialog.create.account.last.name"));
        lastNameField.setValue(registeredUser.getLastName() != null ? registeredUser.getLastName() : "");

        countryField = new TextField(I18n.t("ims.registered.dialog.create.account.country"));
    }

    private Component buildLayout() {
        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add(new Tab(I18n.t("ims.registered.dialog.create.account.tab.tenant")), buildTenantTab());
        tabs.add(new Tab(I18n.t("ims.registered.dialog.create.account.tab.account")), buildAccountTab());

        VerticalLayout root = DialogLayout.stack();
        root.add(buildUserSummary(), tabs);
        return root;
    }

    private Component buildUserSummary() {
        String fullName = ((registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "") + " "
                + (registeredUser.getLastName() != null ? registeredUser.getLastName() : "")).trim();

        List<Component> chips = new ArrayList<>();
        if (registeredUser.getOrganisation() != null && !registeredUser.getOrganisation().isBlank()) {
            chips.add(chip(registeredUser.getOrganisation()));
        }
        if (registeredUser.getPhoneNumber() != null && !registeredUser.getPhoneNumber().isBlank()) {
            chips.add(chip(registeredUser.getPhoneNumber()));
        }
        return new DetailHero(null, fullName, registeredUser.getEmail(), chips.toArray(Component[]::new));
    }

    private static Span chip(String text) {
        Span chip = new Span(text);
        chip.addClassName(DialogLayout.CLASS_CHIP);
        return chip;
    }

    private Component buildTenantTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.registered.dialog.create.account.tenant.section"), VaadinIcon.BUILDING);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(tenantIndustryField, tenantUrlField);
        form.add(tenantAddressField, tenantAdminStatusCombo);
        form.add(tenantDescriptionField, 2);
        section.add(form);
        return section;
    }

    private Component buildAccountTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.registered.dialog.create.account.account.section"), VaadinIcon.USER_CARD);
        FormLayout form = DialogLayout.responsiveForm();
        form.add(accountTypeCombo, languageCombo);
        form.add(functionRoleField, adminStatusCombo);
        form.add(isAdminCheckbox, 2);
        form.add(firstNameField, lastNameField);
        form.add(countryField, 2);
        section.add(form);
        return section;
    }

    @Override
    protected boolean onOk() {
        if (functionRoleField.getValue().isBlank()) {
            append(I18n.t("ims.account.dialog.function.role.required"));
            return false;
        }

        CreateAccountFromRegisteredRequestDto.TenantInfo tenantInfo =
                CreateAccountFromRegisteredRequestDto.TenantInfo.builder()
                        .industry(tenantIndustryField.getValue())
                        .url(tenantUrlField.getValue())
                        .description(tenantDescriptionField.getValue())
                        .adminStatus(tenantAdminStatusCombo.getValue() != null
                                ? tenantAdminStatusCombo.getValue()
                                : IEnumEnabledBinaryStatus.Types.ENABLED)
                        .address(tenantAddressField.getValue())
                        .build();

        CreateAccountFromRegisteredRequestDto.AccountInfo accountInfo =
                CreateAccountFromRegisteredRequestDto.AccountInfo.builder()
                        .accountType(accountTypeCombo.getValue())
                        .language(languageCombo.getValue())
                        .functionalRole(functionRoleField.getValue())
                        .isAdmin(isAdminCheckbox.getValue())
                        .adminStatus(adminStatusCombo.getValue())
                        .accountDetails(AccountDetailsDto.builder()
                                .firstName(firstNameField.getValue())
                                .lastName(lastNameField.getValue())
                                .country(countryField.getValue().isBlank() ? null : countryField.getValue())
                                .build())
                        .build();

        CreateAccountFromRegisteredRequestDto request =
                CreateAccountFromRegisteredRequestDto.builder()
                        .email(registeredUser.getEmail())
                        .tenantInfo(tenantInfo)
                        .accountInfo(accountInfo)
                        .build();

        parentView.showLoading(true);
        try {
            ResponseEntity<AccountDto> response = registeredUserService.createAccountFromRegistered(request);

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
            return true;

        } catch (FeignException ex) {
            append(RegisteredDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("ims.registered.dialog.create.account.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
