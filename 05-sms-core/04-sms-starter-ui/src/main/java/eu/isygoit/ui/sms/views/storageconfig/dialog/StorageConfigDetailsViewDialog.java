package eu.isygoit.ui.sms.views.storageconfig.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.sms.views.common.SmsDetailsDialog;
import eu.isygoit.ui.sms.views.common.SmsDialogSupport;
import eu.isygoit.ui.sms.views.common.SmsEnumTag;
import eu.isygoit.ui.sms.views.storageconfig.StorageConfigManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * Read-only view of a {@link StorageConfigDto}: tenant, type, username and URL,
 * plus the audit tab. {@code id} is never displayed and the password is never shown.
 */
public class StorageConfigDetailsViewDialog extends SmsDetailsDialog {

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

        applyWidth(DialogLayout.WIDTH_M);
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
            add(new Span(I18n.t("sms.storageconfig.details.load.error", SmsDialogSupport.extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("sms.storageconfig.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(StorageConfigDto config) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("sms.storageconfig.details.field.tenant"),
                dash(config.getTenant()));
        grid.add(SmsEnumTag.detailField(
                VaadinIcon.COG,
                I18n.t("sms.storageconfig.details.field.type"),
                SmsEnumTag.ofOrUnknown(config.getType(), "sms.enum.storage")));
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("sms.storageconfig.details.field.username"),
                dash(config.getUserName()));
        // The endpoint is something users copy elsewhere: always offer the copy button.
        addFieldToGrid(grid, VaadinIcon.LINK, I18n.t("sms.storageconfig.details.field.url"),
                dash(config.getUrl()), true);
        addTab(I18n.t("sms.storageconfig.details.section.connection"),
                createSection(I18n.t("sms.storageconfig.details.section.connection"), grid));

        addAuditTab(config.getCreatedBy(), SmsDialogSupport.formatDateTime(config.getCreateDate()),
                config.getUpdatedBy(), SmsDialogSupport.formatDateTime(config.getUpdateDate()));
    }
}
