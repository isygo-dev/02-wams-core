package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumAccountOrigin;
import eu.isygoit.enums.IEnumRegistrationStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;

/**
 * Shared form of the registered-user create/update dialogs: every editable
 * {@link RegisteredUserDto} field is declared here once, in two sections
 * (identity, classification) without tabs (nine fields only). {@code id} is
 * never shown. Subclasses only decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractRegisteredUserFormDialog extends ImsActionDialog {

    final RegisteredManagementView parentView;
    final RegisteredUserService registeredUserService;
    private final String messagePrefix;

    /* identity */
    TextField firstNameField;
    TextField lastNameField;
    EmailField emailField;
    TextField phoneField;

    /* classification */
    TextField organisationField;
    TextField functionRoleField;
    TextField tenantField;
    ComboBox<IEnumAccountOrigin.Types> originCombo;
    ComboBox<IEnumRegistrationStatus.Types> statusCombo;

    AbstractRegisteredUserFormDialog(String title,
                                     Runnable onSuccess,
                                     String messagePrefix,
                                     RegisteredManagementView parentView,
                                     RegisteredUserService registeredUserService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
    }

    /* Hooks */

    /** Whether the e-mail can be edited (create only). */
    abstract boolean emailEditable();

    /** The DTO that receives the form values on save. */
    abstract RegisteredUserDto target();

    /** Persists the DTO; returns false after appending an error. */
    abstract boolean persist(RegisteredUserDto dto);

    /* Layout */

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        VerticalLayout root = DialogLayout.stack();
        root.add(buildIdentitySection(), buildClassificationSection());
        addContent(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.registered.details.section.identity"), VaadinIcon.USER);
        FormLayout form = DialogLayout.responsiveForm();

        firstNameField = new TextField(I18n.t("ims.registered.dialog.field.first.name"));
        firstNameField.setRequiredIndicatorVisible(true);
        firstNameField.setPlaceholder(I18n.t("ims.registered.dialog.field.first.name.placeholder"));

        lastNameField = new TextField(I18n.t("ims.registered.dialog.field.last.name"));
        lastNameField.setRequiredIndicatorVisible(true);
        lastNameField.setPlaceholder(I18n.t("ims.registered.dialog.field.last.name.placeholder"));

        emailField = new EmailField(I18n.t(emailEditable()
                ? "ims.registered.dialog.field.email"
                : "ims.registered.dialog.update.field.email"));
        emailField.setRequiredIndicatorVisible(emailEditable());
        emailField.setReadOnly(!emailEditable());
        emailField.setPlaceholder(I18n.t("ims.registered.dialog.field.email.placeholder"));

        phoneField = new TextField(I18n.t("ims.registered.dialog.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setPlaceholder(I18n.t("ims.registered.dialog.field.phone.placeholder"));

        form.add(firstNameField, lastNameField);
        form.add(emailField, phoneField);

        section.add(form);
        return section;
    }

    private VerticalLayout buildClassificationSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.registered.details.section.classification"), VaadinIcon.SITEMAP);
        FormLayout form = DialogLayout.responsiveForm();

        organisationField = new TextField(I18n.t("ims.registered.dialog.field.organisation"));
        organisationField.setRequiredIndicatorVisible(true);

        functionRoleField = new TextField(I18n.t("ims.registered.dialog.field.function.role"));
        functionRoleField.setPlaceholder(I18n.t("ims.registered.dialog.field.function.role.placeholder"));

        tenantField = new TextField(I18n.t("ims.registered.dialog.field.tenant"));

        originCombo = new ComboBox<>(I18n.t("ims.registered.dialog.field.origin"));
        originCombo.setItems(IEnumAccountOrigin.Types.values());
        originCombo.setItemLabelGenerator(origin -> ImsEnumTag.label(origin, "ims.enum.origin"));
        originCombo.setRenderer(ImsEnumTag.renderer("ims.enum.origin"));

        statusCombo = new ComboBox<>(I18n.t("ims.registered.dialog.field.status"));
        statusCombo.setItems(IEnumRegistrationStatus.Types.values());
        statusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, "ims.registered.card.status"));
        statusCombo.setRenderer(ImsEnumTag.renderer("ims.registered.card.status"));

        form.add(organisationField, functionRoleField);
        form.add(tenantField, originCombo);
        form.add(statusCombo, 2);

        section.add(form);
        return section;
    }

    /* DTO <-> form */

    /** Fills every field from the DTO (initial values on create, loaded values on update). */
    final void fillFrom(RegisteredUserDto dto) {
        firstNameField.setValue(nvl(dto.getFirstName()));
        lastNameField.setValue(nvl(dto.getLastName()));
        emailField.setValue(nvl(dto.getEmail()));
        phoneField.setValue(nvl(dto.getPhoneNumber()));

        organisationField.setValue(nvl(dto.getOrganisation()));
        functionRoleField.setValue(nvl(dto.getFunctionRole()));
        tenantField.setValue(nvl(dto.getTenant()));
        if (dto.getOrigin() != null) {
            try {
                originCombo.setValue(dto.getOrigin());
            } catch (IllegalArgumentException ignored) {
                // origin not offered by the combo: leave it empty
            }
        }
        statusCombo.setValue(dto.getStatus());
    }

    /** Copies every field into the DTO. {@code id} is left untouched. */
    private void applyTo(RegisteredUserDto dto) {
        dto.setFirstName(firstNameField.getValue());
        dto.setLastName(lastNameField.getValue());
        if (emailEditable()) {
            dto.setEmail(emailField.getValue());
        }
        dto.setPhoneNumber(phoneField.getValue());
        dto.setOrganisation(organisationField.getValue());
        dto.setFunctionRole(functionRoleField.getValue());
        if (!tenantField.getValue().isBlank()) {
            dto.setTenant(tenantField.getValue().trim());
        }
        if (originCombo.getValue() != null) {
            dto.setOrigin(originCombo.getValue());
        }
        if (statusCombo.getValue() != null) {
            dto.setStatus(statusCombo.getValue());
        }
    }

    private boolean isValid() {
        if (firstNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.first.name.required"));
            return false;
        }
        if (lastNameField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.last.name.required"));
            return false;
        }
        if (emailEditable() && emailField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.phone.required"));
            return false;
        }
        if (organisationField.getValue().isBlank()) {
            append(I18n.t("ims.registered.dialog.field.organisation.required"));
            return false;
        }
        return true;
    }

    /* Save */

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            RegisteredUserDto dto = target();
            applyTo(dto);

            if (!persist(dto)) {
                return false;
            }
            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(RegisteredDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
