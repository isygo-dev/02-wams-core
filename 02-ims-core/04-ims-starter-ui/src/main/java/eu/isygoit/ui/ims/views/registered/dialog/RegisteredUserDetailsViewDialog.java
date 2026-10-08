package eu.isygoit.ui.ims.views.registered.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import eu.isygoit.ui.ims.views.registered.RegisteredManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class RegisteredUserDetailsViewDialog extends DetailsViewDialog {

    private final RegisteredManagementView parentView;
    private final RegisteredUserService registeredUserService;
    private final Long registeredUserId;

    public RegisteredUserDetailsViewDialog(RegisteredManagementView parentView,
                                           RegisteredUserService registeredUserService,
                                           Long registeredUserId) {
        super(I18n.t("ims.registered.details.title"));
        this.parentView = parentView;
        this.registeredUserService = registeredUserService;
        this.registeredUserId = registeredUserId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("registered-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<RegisteredUserDto> response = registeredUserService.findById(registeredUserId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.registered.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.registered.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.registered.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(RegisteredUserDto registeredUser) {
        // Identity — name/email/phone/organisation
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        String fullName = (registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "") +
                " " + (registeredUser.getLastName() != null ? registeredUser.getLastName() : "");
        addFieldToGrid(identityInfo, VaadinIcon.USER, I18n.t("ims.registered.details.field.name"), fullName.trim(), false);
        addFieldToGrid(identityInfo, VaadinIcon.ENVELOPE, I18n.t("ims.registered.details.field.email"), registeredUser.getEmail(), true);
        addFieldToGrid(identityInfo, VaadinIcon.PHONE, I18n.t("ims.registered.details.field.phone"), registeredUser.getPhoneNumber());
        addFieldToGrid(identityInfo, VaadinIcon.BUILDING, I18n.t("ims.registered.details.field.organisation"), registeredUser.getOrganisation());

        add(createSection(I18n.t("ims.registered.details.section.identity"), identityInfo));

        // Classification & status — origin, function role, status
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.SITEMAP, I18n.t("ims.registered.details.field.origin"),
                registeredUser.getOrigin(), "ims.enum.origin");
        addFieldToGrid(classificationInfo, VaadinIcon.BRIEFCASE, I18n.t("ims.registered.details.field.function.role"),
                registeredUser.getFunctionRole());

        // Status chip field
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.registered.details.field.status"),
                registeredUser.getStatus(), "ims.registered.card.status");

        add(createSection(I18n.t("ims.registered.details.section.classification"), classificationInfo));

        // Contact / relations — tenant
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING_O, I18n.t("ims.registered.details.field.tenant"), registeredUser.getTenant(), true);

        add(createSection(I18n.t("ims.registered.details.section.contact"), contactInfo));

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.registered.details.field.created"), registeredUser.getCreateDate() != null ? DateHelper.formatToHumanReadable(registeredUser.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.registered.details.field.created.by"), registeredUser.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.registered.details.field.updated"), registeredUser.getUpdateDate() != null ? DateHelper.formatToHumanReadable(registeredUser.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.registered.details.field.updated.by"), registeredUser.getUpdatedBy());

        add(createSection(I18n.t("ims.registered.details.section.audit"), auditInfo));

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