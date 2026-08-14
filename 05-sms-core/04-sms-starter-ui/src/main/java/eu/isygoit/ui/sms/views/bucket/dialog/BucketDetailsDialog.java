package eu.isygoit.ui.sms.views.bucket.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;

public class BucketDetailsDialog extends DetailsViewDialog {

    private final BucketManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final BucketDto bucket;

    public BucketDetailsDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                               String tenant, BucketDto bucket) {
        super(I18n.t("sms.buckets.details.title"));
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucket = bucket;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("bucket-details-dialog");

        buildContent(bucket);
    }

    private void buildContent(BucketDto bucket) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.FOLDER, I18n.t("sms.buckets.details.field.name"), bucket.getName(), true);
        addFieldToGrid(identityGrid, VaadinIcon.CALENDAR, I18n.t("sms.buckets.details.field.created"),
                bucket.getCreationDate() != null ? bucket.getCreationDate().toString() : null);
        mainLayout.add(createSection(I18n.t("sms.buckets.details.section.identity"), identityGrid));

        add(mainLayout);
    }
}