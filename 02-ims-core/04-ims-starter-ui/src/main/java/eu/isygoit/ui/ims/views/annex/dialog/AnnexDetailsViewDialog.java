package eu.isygoit.ui.ims.views.annex.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.AnnexDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AnnexService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.ims.views.annex.AnnexManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class AnnexDetailsViewDialog extends DetailsViewDialog {

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

        setWidth("700px");
        setMaxWidth("95%");
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
            add(new Span(I18n.t("ims.annex.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("ims.annex.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(AnnexDto annex) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity — table code/value/reference/order (text identifiers)
        Div identityInfo = new Div();
        identityInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityInfo, VaadinIcon.CODE, I18n.t("ims.annex.details.field.table.code"), annex.getTableCode(), true);
        addFieldToGrid(identityInfo, VaadinIcon.FONT, I18n.t("ims.annex.details.field.value"), annex.getValue(), true);
        addFieldToGrid(identityInfo, VaadinIcon.LINK, I18n.t("ims.annex.details.field.reference"), annex.getReference(), true);
        addFieldToGrid(identityInfo, VaadinIcon.SORT, I18n.t("ims.annex.details.field.order"), annex.getAnnexOrder() != null ? String.valueOf(annex.getAnnexOrder()) : null);

        mainLayout.add(createSection(I18n.t("ims.annex.details.section.identity"), identityInfo));

        // Classification & status — language
        Div classificationInfo = new Div();
        classificationInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(classificationInfo, VaadinIcon.LOCATION_ARROW_CIRCLE, I18n.t("ims.annex.details.field.language"), annex.getLanguage() != null ? annex.getLanguage().name() : null);

        mainLayout.add(createSection(I18n.t("ims.annex.details.section.classification"), classificationInfo));

        // Contact / relations — tenant/description
        Div contactInfo = new Div();
        contactInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(contactInfo, VaadinIcon.BUILDING, I18n.t("ims.annex.details.field.tenant"), annex.getTenant(), true);
        addFieldToGrid(contactInfo, VaadinIcon.FILE_TEXT, I18n.t("ims.annex.details.field.description"), annex.getDescription(), false);

        mainLayout.add(createSection(I18n.t("ims.annex.details.section.contact"), contactInfo));

        // Audit — created/updated by & date
        Div auditInfo = new Div();
        auditInfo.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR, I18n.t("ims.annex.details.field.created"), annex.getCreateDate() != null ? DateHelper.formatToHumanReadable(annex.getCreateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.USER_CHECK, I18n.t("ims.annex.details.field.created.by"), annex.getCreatedBy());
        addFieldToGrid(auditInfo, VaadinIcon.CALENDAR_O, I18n.t("ims.annex.details.field.updated"), annex.getUpdateDate() != null ? DateHelper.formatToHumanReadable(annex.getUpdateDate()) : null);
        addFieldToGrid(auditInfo, VaadinIcon.EDIT, I18n.t("ims.annex.details.field.updated.by"), annex.getUpdatedBy());

        mainLayout.add(createSection(I18n.t("ims.annex.details.section.audit"), auditInfo));

        add(mainLayout);
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