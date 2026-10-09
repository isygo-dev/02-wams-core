package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import eu.isygoit.ui.ims.views.common.ImsDetailsDialog;
import eu.isygoit.ui.ims.views.common.ImsDialogSupport;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public class AnnexDetailsViewDialog extends ImsDetailsDialog {

    private final AnnexManagementView parentView;
    private final AnnexService annexService;
    private final Long annexId;

    public AnnexDetailsViewDialog(AnnexManagementView parentView,
                                  AnnexService annexService,
                                  Long annexId) {
        super(I18n.t("ims.annex.details.title"));
        this.parentView = parentView;
        this.annexService = annexService;
        this.annexId = annexId;

        applyWidth(DialogLayout.WIDTH_M);
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("annex-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<AnnexDto> response = annexService.findById(annexId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("ims.annex.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("ims.annex.details.load.error", ImsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.annex.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AnnexDto annex) {
        Div identityInfo = createDetailGrid();
        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.annex.details.field.table.code"), annex.getTableCode(), true);
        addFieldToGrid(identityInfo, VaadinIcon.FONT, I18n.t("ims.annex.details.field.value"), annex.getValue(), true);
        addFieldToGrid(identityInfo, VaadinIcon.LINK, I18n.t("ims.annex.details.field.reference"), annex.getReference(), true);
        addFieldToGrid(identityInfo, VaadinIcon.SORT, I18n.t("ims.annex.details.field.order"), annex.getAnnexOrder() != null ? String.valueOf(annex.getAnnexOrder()) : null);
        ImsEnumTag.addDetailField(identityInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.annex.details.field.language"), annex.getLanguage(), "ims.enum.language");
        addFieldToGrid(identityInfo, VaadinIcon.BUILDING, I18n.t("ims.annex.details.field.tenant"), annex.getTenant(), true);
        addFieldToGrid(identityInfo, VaadinIcon.FILE_TEXT, I18n.t("ims.annex.details.field.description"), annex.getDescription(), false);
        addTab(I18n.t("ims.annex.details.section.identity"), createSection(I18n.t("ims.annex.details.section.identity"), identityInfo));

        addAuditTab(annex.getCreatedBy(), formatDate(annex.getCreateDate()),
                annex.getUpdatedBy(), formatDate(annex.getUpdateDate()));
    }

    private static String formatDate(LocalDateTime date) {
        return date != null ? DateHelper.formatToHumanReadable(date) : null;
    }
}
