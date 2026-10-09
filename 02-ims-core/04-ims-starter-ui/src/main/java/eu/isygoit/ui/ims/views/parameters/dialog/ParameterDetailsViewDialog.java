package eu.isygoit.ui.ims.views.parameters.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AppParameterDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AppParameterService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.parameters.ParameterManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class ParameterDetailsViewDialog extends ImsDetailsDialog {

    private final ParameterManagementView parentView;
    private final AppParameterService parameterService;
    private final Long parameterId;

    public ParameterDetailsViewDialog(ParameterManagementView parentView,
                                      AppParameterService parameterService,
                                      Long parameterId) {
        super(I18n.t("ims.parameter.details.title"));
        this.parentView = parentView;
        this.parameterService = parameterService;
        this.parameterId = parameterId;

        applyWidth(DialogLayout.WIDTH_M);
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("parameter-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AppParameterDto> response = parameterService.findById(parameterId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.parameter.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.parameter.details.load.error", ImsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.parameter.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AppParameterDto param) {
        Div identityInfo = createDetailGrid();
        addFieldToGrid(identityInfo, VaadinIcon.KEY, I18n.t("ims.parameter.details.field.name"), param.getName(), true);
        addFieldToGrid(identityInfo, VaadinIcon.INPUT, I18n.t("ims.parameter.details.field.value"), param.getValue(), true);
        addFieldToGrid(identityInfo, VaadinIcon.BUILDING, I18n.t("ims.parameter.details.field.tenant"), param.getTenant(), true);
        addFieldToGrid(identityInfo, VaadinIcon.FILE_TEXT, I18n.t("ims.parameter.details.field.description"), param.getDescription(), false);
        addTab(I18n.t("ims.parameter.details.section.identity"), createSection(I18n.t("ims.parameter.details.section.identity"), identityInfo));

        addAuditTab(param.getCreatedBy(), formatDate(param.getCreateDate()),
                param.getUpdatedBy(), formatDate(param.getUpdateDate()));
    }

    private static String formatDate(LocalDateTime date) {
        return date != null ? DateHelper.formatToHumanReadable(date) : null;
    }
}
