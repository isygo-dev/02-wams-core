package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class StorageConfigDetailsViewDialog extends DetailsViewDialog {

    private final StorageConfigManagementView parentView;
    private final StorageConfigService storageConfigService;
    private final Long configId;

    public StorageConfigDetailsViewDialog(StorageConfigManagementView parentView,
                                          StorageConfigService storageConfigService,
                                          Long configId) {
        super(I18n.t("sms.storageconfig.details.title"));
        this.parentView = parentView;
        this.storageConfigService = storageConfigService;
        this.configId = configId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("storageconfig-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<StorageConfigDto> response = storageConfigService.findById(configId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("sms.storageconfig.details.not.found")));
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("sms.storageconfig.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("sms.storageconfig.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(StorageConfigDto config) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = createDetailGrid();
        // ID is a copyable identifier, force the copy button on even though it's short.
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("sms.storageconfig.details.field.id"), String.valueOf(config.getId()), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("sms.storageconfig.details.field.tenant"), config.getTenant());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("sms.storageconfig.details.field.type"), config.getType() != null ? config.getType().name() : null);
        addFieldToGrid(identityGrid, VaadinIcon.USER, I18n.t("sms.storageconfig.details.field.username"), config.getUserName());
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.identity"), identityGrid));

        Div connectionGrid = createDetailGrid();
        // URL is a connection endpoint users would copy/paste elsewhere — force copyable.
        addFieldToGrid(connectionGrid, VaadinIcon.LINK, I18n.t("sms.storageconfig.details.field.url"), config.getUrl(), true);
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.connection"), connectionGrid));

        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("sms.storageconfig.details.field.created.by"), config.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("sms.storageconfig.details.field.created.date"),
                config.getCreateDate() != null ? DateHelper.formatToHumanReadable(config.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("sms.storageconfig.details.field.updated.by"), config.getUpdatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("sms.storageconfig.details.field.updated.date"),
                config.getUpdateDate() != null ? DateHelper.formatToHumanReadable(config.getUpdateDate()) : null);
        mainLayout.add(createSection(I18n.t("sms.storageconfig.details.section.audit"), auditGrid));

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
