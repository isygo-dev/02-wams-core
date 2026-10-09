package eu.isygoit.ui.sms.views.bucket.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;
import eu.isygoit.ui.sms.views.common.SmsDetailsDialog;
import eu.isygoit.ui.sms.views.common.SmsDialogSupport;

/**
 * Read-only view of a {@link BucketDto}: name, creation date, region and ARN
 * (the DTO carries no audit fields).
 */
public class BucketDetailsDialog extends SmsDetailsDialog {

    public BucketDetailsDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                               String tenant, BucketDto bucket) {
        super(I18n.t("sms.buckets.details.title"));
        applyWidth(DialogLayout.WIDTH_S);
        buildContent(bucket);
    }

    private void buildContent(BucketDto bucket) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FOLDER, I18n.t("sms.buckets.details.field.name"),
                dash(bucket.getName()), true);
        addFieldToGrid(grid, VaadinIcon.CALENDAR, I18n.t("sms.buckets.details.field.created"),
                dash(SmsDialogSupport.formatDateTime(bucket.getCreationDate())));
        addFieldToGrid(grid, VaadinIcon.GLOBE, I18n.t("sms.buckets.details.field.region"),
                dash(bucket.getBucketRegion()));
        addFieldToGrid(grid, VaadinIcon.CODE, I18n.t("sms.buckets.details.field.arn"),
                dash(bucket.getBucketArn()), true);
        add(createSection(I18n.t("sms.buckets.details.title"), grid));
    }
}
