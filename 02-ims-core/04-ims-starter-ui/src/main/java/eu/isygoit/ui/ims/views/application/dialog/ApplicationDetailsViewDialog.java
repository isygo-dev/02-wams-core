package eu.isygoit.ui.ims.views.application.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.ApplicationDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.ApplicationService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.application.ApplicationManagementView;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class ApplicationDetailsViewDialog extends ImsDetailsDialog {

    private final ApplicationManagementView parentView;
    private final ApplicationService applicationService;
    private final Long applicationId;

    public ApplicationDetailsViewDialog(ApplicationManagementView parentView,
                                        ApplicationService applicationService,
                                        Long applicationId) {
        super(I18n.t("ims.app.details.title"));
        this.parentView = parentView;
        this.applicationService = applicationService;
        this.applicationId = applicationId;

        applyWidth(DialogLayout.WIDTH_M);
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("application-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<ApplicationDto> response = applicationService.findById(applicationId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.app.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.app.details.load.error", ImsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.app.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(ApplicationDto app) {
        // Identity: name, title, code, tenant, url
        Div identityInfo = createDetailGrid();
        addFieldToGrid(identityInfo, VaadinIcon.PLAY, I18n.t("ims.app.details.field.name"), app.getName());
        addFieldToGrid(identityInfo, VaadinIcon.FUNCTION, I18n.t("ims.app.details.field.title"), app.getTitle());
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.app.details.field.code"), app.getCode(), true);
        addFieldToGrid(identityInfo, VaadinIcon.BUILDING, I18n.t("ims.app.details.field.tenant"), app.getTenant(), true);
        addFieldToGrid(identityInfo, VaadinIcon.GLOBE, I18n.t("ims.app.details.field.url"), app.getUrl(), true);
        addTab(I18n.t("ims.app.details.section.identity"), createSection(I18n.t("ims.app.details.section.identity"), identityInfo));

        // Classification and status: category, order, admin status, description
        Div classificationInfo = createDetailGrid();
        addFieldToGrid(classificationInfo, VaadinIcon.DESKTOP, I18n.t("ims.app.details.field.category"), app.getCategory());
        addFieldToGrid(classificationInfo, VaadinIcon.SORT, I18n.t("ims.app.details.field.order"), app.getOrder() != null ? String.valueOf(app.getOrder()) : null);
        ImsEnumTag.addDetailField(classificationInfo, VaadinIcon.SHIELD, I18n.t("ims.app.details.field.admin.status"), app.getAdminStatus(), null);
        addFieldToGrid(classificationInfo, VaadinIcon.FILE_TEXT, I18n.t("ims.app.details.field.description"), app.getDescription(), false);
        addTab(I18n.t("ims.app.details.section.classification"), createSection(I18n.t("ims.app.details.section.classification"), classificationInfo));

        addAuditTab(app.getCreatedBy(), formatDate(app.getCreateDate()),
                app.getUpdatedBy(), formatDate(app.getUpdateDate()));
    }

    private static String formatDate(LocalDateTime date) {
        return date != null ? DateHelper.formatToHumanReadable(date) : null;
    }
}
