package eu.isygoit.ui.ims.views.tenant.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.common.dialog.ProfilePhotoSection;
import eu.isygoit.ui.ims.views.common.ImsActionDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.tenant.TenantManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Shared form of the tenant create/update dialogs: every editable {@link TenantDto}
 * field is declared here once, in three tabs (identity, contact, address). {@code id}
 * is never shown and {@code code} is always read-only (never sent). Subclasses only
 * decide how the DTO is persisted.
 *
 * <p>Subclasses must call {@link #buildForm()} at the end of their constructor.
 */
abstract class AbstractTenantFormDialog extends ImsActionDialog {

    final TenantManagementView parentView;
    final TenantService tenantService;
    final TenantImageService tenantImageService;
    private final String messagePrefix;

    /* identity */
    TextField codeField;
    TextField nameField;
    TextField industryField;
    ComboBox<IEnumEnabledBinaryStatus.Types> adminStatusCombo;
    TextArea descriptionField;

    /* contact */
    EmailField emailField;
    TextField phoneField;
    TextField urlField;
    TextField facebookField;
    TextField linkedinField;
    TextField xingField;

    /* address */
    TextField countryField;
    TextField stateField;
    TextField cityField;
    TextField streetField;
    TextField zipCodeField;
    TextField additionalInfoField;

    ProfilePhotoSection photoSection;

    AbstractTenantFormDialog(String title,
                             Runnable onSuccess,
                             String messagePrefix,
                             TenantManagementView parentView,
                             TenantService tenantService,
                             TenantImageService tenantImageService) {
        super(title, onSuccess);
        this.messagePrefix = messagePrefix;
        this.parentView = parentView;
        this.tenantService = tenantService;
        this.tenantImageService = tenantImageService;
    }

    /* Hooks */

    /** I18n key of the "upload / change image" button. */
    abstract String photoButtonKey();

    /** Whether a newly cropped image is mandatory to save. */
    abstract boolean imageRequired();

    /** The DTO that receives the form values on save. */
    abstract TenantDto target();

    /** Persists the DTO; returns the stored tenant id, or null after appending an error. */
    abstract Long persist(TenantDto dto);

    /* Layout */

    final void buildForm() {
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        photoSection = new ProfilePhotoSection(
                I18n.t("ims.tenant.dialog.field.image"),
                I18n.t("ims.tenant.dialog.field.image.help"),
                I18n.t(photoButtonKey()),
                I18n.t("ims.tenant.dialog.field.remove.image"));

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.add(new Tab(I18n.t("ims.tenant.dialog.tab.identity")), buildIdentityTab());
        tabs.add(new Tab(I18n.t("ims.tenant.dialog.tab.contact")), buildContactTab());
        tabs.add(new Tab(I18n.t("ims.tenant.dialog.tab.address")), buildAddressTab());

        VerticalLayout root = DialogLayout.stack();
        root.add(photoSection, tabs);
        addContent(root);
    }

    private Component buildIdentityTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.tenant.details.section.identity"), VaadinIcon.BUILDING);
        FormLayout form = DialogLayout.responsiveForm();

        nameField = new TextField(I18n.t("ims.tenant.dialog.field.name"));
        nameField.setRequiredIndicatorVisible(true);
        nameField.setPlaceholder(I18n.t("ims.tenant.dialog.field.name.placeholder"));

        codeField = new TextField(I18n.t("ims.tenant.details.field.code"));
        codeField.setReadOnly(true);

        industryField = new TextField(I18n.t("ims.tenant.dialog.field.industry"));
        industryField.setPlaceholder(I18n.t("ims.tenant.dialog.field.industry.placeholder"));

        adminStatusCombo = new ComboBox<>(I18n.t("ims.tenant.dialog.field.admin.status"));
        adminStatusCombo.setItems(IEnumEnabledBinaryStatus.Types.values());
        adminStatusCombo.setItemLabelGenerator(status -> ImsEnumTag.label(status, null));
        adminStatusCombo.setRenderer(ImsEnumTag.renderer(null));

        descriptionField = DialogLayout.tall(new TextArea(I18n.t("ims.tenant.dialog.field.description")));
        descriptionField.setPlaceholder(I18n.t("ims.tenant.dialog.field.description.placeholder"));

        form.add(nameField, codeField);
        form.add(industryField, adminStatusCombo);
        form.add(descriptionField, 2);

        section.add(form);
        return section;
    }

    private Component buildContactTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.tenant.details.section.contact"), VaadinIcon.PHONE);
        FormLayout form = DialogLayout.responsiveForm();

        emailField = new EmailField(I18n.t("ims.tenant.dialog.field.email"));
        emailField.setRequiredIndicatorVisible(true);
        emailField.setPlaceholder(I18n.t("ims.tenant.dialog.field.email.placeholder"));

        phoneField = new TextField(I18n.t("ims.tenant.dialog.field.phone"));
        phoneField.setRequiredIndicatorVisible(true);
        phoneField.setPlaceholder(I18n.t("ims.tenant.dialog.field.phone.placeholder"));

        urlField = new TextField(I18n.t("ims.tenant.dialog.field.website"));
        urlField.setPlaceholder(I18n.t("ims.tenant.dialog.field.website.placeholder"));

        facebookField = new TextField(I18n.t("ims.tenant.details.field.facebook"));
        linkedinField = new TextField(I18n.t("ims.tenant.details.field.linkedin"));
        xingField = new TextField(I18n.t("ims.tenant.details.field.xing"));

        form.add(emailField, phoneField);
        form.add(urlField, 2);
        form.add(facebookField, linkedinField);
        form.add(xingField, 2);

        section.add(form);
        return section;
    }

    private Component buildAddressTab() {
        VerticalLayout section = DialogLayout.section(
                I18n.t("ims.tenant.details.section.address"), VaadinIcon.MAP_MARKER);
        FormLayout form = DialogLayout.responsiveForm();

        countryField = new TextField(I18n.t("ims.tenant.details.field.country"));
        stateField = new TextField(I18n.t("ims.tenant.details.field.state"));
        cityField = new TextField(I18n.t("ims.tenant.details.field.city"));
        streetField = new TextField(I18n.t("ims.tenant.details.field.street"));
        zipCodeField = new TextField(I18n.t("ims.tenant.details.field.zip.code"));
        additionalInfoField = new TextField(I18n.t("ims.tenant.details.field.additional.info"));

        form.add(countryField, stateField);
        form.add(cityField, streetField);
        form.add(zipCodeField, additionalInfoField);

        section.add(form);
        return section;
    }

    /* DTO <-> form */

    /** Fills every field from the DTO (update flow). */
    final void fillFrom(TenantDto dto) {
        codeField.setValue(nvl(dto.getCode()));
        nameField.setValue(nvl(dto.getName()));
        industryField.setValue(nvl(dto.getIndustry()));
        adminStatusCombo.setValue(dto.getAdminStatus());
        descriptionField.setValue(nvl(dto.getDescription()));

        emailField.setValue(nvl(dto.getEmail()));
        phoneField.setValue(nvl(dto.getPhone()));
        urlField.setValue(nvl(dto.getUrl()));
        facebookField.setValue(nvl(dto.getLnk_facebook()));
        linkedinField.setValue(nvl(dto.getLnk_linkedin()));
        xingField.setValue(nvl(dto.getLnk_xing()));

        AddressDto addr = dto.getAddress();
        if (addr != null) {
            countryField.setValue(nvl(addr.getCountry()));
            stateField.setValue(nvl(addr.getState()));
            cityField.setValue(nvl(addr.getCity()));
            streetField.setValue(nvl(addr.getStreet()));
            zipCodeField.setValue(nvl(addr.getZipCode()));
            additionalInfoField.setValue(nvl(addr.getAdditionalInfo()));
        }
    }

    /** Copies every editable field into the DTO. {@code id} and {@code code} are left untouched. */
    private void applyTo(TenantDto dto) {
        dto.setName(nameField.getValue());
        dto.setIndustry(industryField.getValue());
        dto.setAdminStatus(adminStatusCombo.getValue());
        dto.setDescription(descriptionField.getValue());

        dto.setEmail(emailField.getValue());
        dto.setPhone(phoneField.getValue());
        dto.setUrl(urlField.getValue());
        dto.setLnk_facebook(facebookField.getValue());
        dto.setLnk_linkedin(linkedinField.getValue());
        dto.setLnk_xing(xingField.getValue());

        dto.setAddress(hasAddressData() ? buildAddress(dto.getAddress()) : null);
    }

    /** Keeps the id and the other address attributes of an existing address (not editable here). */
    private AddressDto buildAddress(AddressDto existing) {
        AddressDto address = existing != null ? existing : AddressDto.builder().build();
        address.setCountry(countryField.getValue());
        address.setState(stateField.getValue());
        address.setCity(cityField.getValue());
        address.setStreet(streetField.getValue());
        address.setZipCode(zipCodeField.getValue());
        address.setAdditionalInfo(additionalInfoField.getValue());
        return address;
    }

    private boolean hasAddressData() {
        return !countryField.getValue().isBlank()
                || !stateField.getValue().isBlank()
                || !cityField.getValue().isBlank()
                || !streetField.getValue().isBlank()
                || !zipCodeField.getValue().isBlank()
                || !additionalInfoField.getValue().isBlank();
    }

    private boolean isValid() {
        if (nameField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.name.required"));
            return false;
        }
        if (emailField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.email.required"));
            return false;
        }
        if (phoneField.getValue().isBlank()) {
            append(I18n.t("ims.tenant.dialog.field.phone.required"));
            return false;
        }
        if (imageRequired() && photoSection.getSelectedFile() == null) {
            append(I18n.t("ims.tenant.dialog.create.image.required"));
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
            TenantDto dto = target();
            applyTo(dto);

            Long tenantId = persist(dto);
            if (tenantId == null) {
                return false;
            }

            if (photoSection.getSelectedFile() != null) {
                ResponseEntity<TenantDto> uploadResponse =
                        tenantImageService.uploadImage(tenantId, photoSection.getSelectedFile());
                if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                    append(I18n.t(messagePrefix + ".image.failed", uploadResponse.getStatusCodeValue()));
                    return false;
                }
            }

            append(I18n.t(messagePrefix + ".success"));
            return true;
        } catch (FeignException ex) {
            append(TenantDialogSupport.extractErrorMessage(ex));
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
