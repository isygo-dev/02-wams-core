package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationImageService;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.ProfilePhotoSection;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shared form of the application create/update dialogs: every editable
 * {@link ApplicationDto} field is declared here once. {@code id} is never shown,
 * {@code code} is always read-only (assigned by the server) and the authorization
 * {@code token} is never displayed. Subclasses only decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractApplicationFormDialog extends ImsActionDialog {

    final ApplicationManagementView parentView;
    final ApplicationService applicationService;
    final ApplicationImageService applicationImageService;
    private final String messagePrefix;

    ProfilePhotoSection photoSection;

    TextField nameField;
    TextField titleField;
    TextField codeField;
    TextField tenantField;
    TextField categoryField;
    TextField urlField;
    IntegerField orderField;
    ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    TextArea descriptionField;

    AbstractApplicationFormDialog(String title,
                                  Runnable onSuccess,
                                  String messagePrefix,
                                  ApplicationManagementView parentView,
                                  ApplicationService applicationService,
                                  ApplicationImageService applicationImageService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationImageService = applicationImageService;
    }

    /** The DTO that receives the form values on save. */
    abstract ApplicationDto target();

    /** Persists the DTO; returns the application id, or null after appending an error message. */
    abstract Long persist(ApplicationDto dto);

    /** i18n key of the photo upload button. */
    abstract String uploadLabelKey();

    /** True when a picture must be selected before saving. */
    abstract boolean imageRequired();

    /** True when the tenant cannot be changed (update). */
    abstract boolean isTenantReadOnly();

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_M);

        photoSection = new ProfilePhotoSection(
                I18n.t("ims.app.dialog.photo.title"),
                I18n.t("ims.app.dialog.photo.help"),
                I18n.t(uploadLabelKey()),
                I18n.t("ims.app.dialog.photo.remove"));

        VerticalLayout root = DialogLayout.stack();
        root.add(photoSection, buildIdentitySection(), buildSettingsSection(), buildDescriptionSection());
        add(root);
    }

    private VerticalLayout buildIdentitySection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.app.dialog.section.identity"), VaadinIcon.DESKTOP);
        FormLayout form = DialogLayout.responsiveForm();

        nameField = new TextField(I18n.t("ims.app.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("ims.app.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        titleField = new TextField(I18n.t("ims.app.dialog.field.title"));
        titleField.setRequiredIndicatorVisible(true);
        titleField.setPlaceholder(I18n.t("ims.app.dialog.field.title.placeholder"));
        titleField.setWidthFull();

        codeField = new TextField(I18n.t("ims.app.dialog.field.code"));
        codeField.setReadOnly(true);
        codeField.setWidthFull();

        tenantField = new TextField(I18n.t("ims.app.dialog.field.tenant"));
        tenantField.setPlaceholder(I18n.t("ims.app.dialog.field.tenant.placeholder"));
        tenantField.setReadOnly(isTenantReadOnly());
        tenantField.setWidthFull();

        form.add(nameField, titleField, codeField, tenantField);
        section.add(form);
        return section;
    }

    private VerticalLayout buildSettingsSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.app.dialog.section.settings"), VaadinIcon.COG);
        FormLayout form = DialogLayout.responsiveForm();

        categoryField = new TextField(I18n.t("ims.app.dialog.field.category"));
        categoryField.setPlaceholder(I18n.t("ims.app.dialog.field.category.placeholder"));
        categoryField.setWidthFull();

        urlField = new TextField(I18n.t("ims.app.dialog.field.url"));
        urlField.setRequiredIndicatorVisible(true);
        urlField.setPlaceholder(I18n.t("ims.app.dialog.field.url.placeholder"));
        urlField.setWidthFull();

        orderField = new IntegerField(I18n.t("ims.app.dialog.field.order"));
        orderField.setPlaceholder(I18n.t("ims.app.dialog.field.order.placeholder"));
        orderField.setWidthFull();

        adminStatusCombo = new ComboBox<>(I18n.t("ims.app.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        adminStatusCombo.setRenderer(ImsEnumTag.renderer(null));
        adminStatusCombo.setWidthFull();

        form.add(categoryField, urlField, orderField, adminStatusCombo);
        section.add(form);
        return section;
    }

    private VerticalLayout buildDescriptionSection() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.app.dialog.section.description"), VaadinIcon.FILE_TEXT);

        descriptionField = DialogLayout.tall(new TextArea(I18n.t("ims.app.dialog.field.description")));
        descriptionField.setPlaceholder(I18n.t("ims.app.dialog.field.description.placeholder"));

        section.add(descriptionField);
        return section;
    }

    /** Fills the form from an existing application. */
    final void fillFrom(ApplicationDto dto) {
        nameField.setValue(dto.getName() != null ? dto.getName() : "");
        titleField.setValue(dto.getTitle() != null ? dto.getTitle() : "");
        codeField.setValue(dto.getCode() != null ? dto.getCode() : "");
        tenantField.setValue(dto.getTenant() != null ? dto.getTenant() : "");
        categoryField.setValue(dto.getCategory() != null ? dto.getCategory() : "");
        urlField.setValue(dto.getUrl() != null ? dto.getUrl() : "");
        orderField.setValue(dto.getOrder());
        descriptionField.setValue(dto.getDescription() != null ? dto.getDescription() : "");
        adminStatusCombo.setValue(dto.getAdminStatus());
    }

    /** Copies the form values into the DTO ({@code id}, {@code code} and {@code token} are left untouched). */
    private void applyTo(ApplicationDto dto) {
        if (!tenantField.isReadOnly()) {
            dto.setTenant(tenantField.getValue().isBlank() ? null : tenantField.getValue());
        }
        dto.setName(nameField.getValue());
        dto.setTitle(titleField.getValue());
        dto.setCategory(categoryField.getValue());
        dto.setUrl(urlField.getValue());
        dto.setOrder(orderField.getValue());
        dto.setDescription(descriptionField.getValue());
        dto.setAdminStatus(adminStatusCombo.getValue());
    }

    private boolean isValid() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.field.name.required"));
            return false;
        }
        if (titleField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.field.title.required"));
            return false;
        }
        if (urlField.getValue().isBlank()) {
            append(I18n.t("ims.app.dialog.field.url.required"));
            return false;
        }
        if (imageRequired() && photoSection.getSelectedFile() == null) {
            append(I18n.t("ims.app.dialog.create.image.required"));
            return false;
        }
        return true;
    }

    @Override
    protected final boolean onOk() {
        if (!isValid()) {
            return false;
        }

        parentView.showLoading(true);
        try {
            ApplicationDto dto = target();
            applyTo(dto);
            Long applicationId = persist(dto);
            if (applicationId == null) {
                return false;
            }

            MultipartFile image = photoSection.getSelectedFile();
            if (image != null) {
                ResponseEntity<ApplicationDto> uploadResponse = applicationImageService.uploadImage(applicationId, image);
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t(messagePrefix + ".image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(ImsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messagePrefix + ".error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
