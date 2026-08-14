package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;

import java.time.format.DateTimeFormatter;

public class FileDetailsDialog extends DetailsViewDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final ObjectStorageManagementView.FileItem file;

    public FileDetailsDialog(ObjectStorageManagementView parentView,
                             ObjectStorageService objectStorageService,
                             String tenant,
                             String bucketName,
                             ObjectStorageManagementView.FileItem file) {
        super(I18n.t("sms.objects.details.title", file.getName()));
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.file = file;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("file-details-dialog");

        buildContent(file);
    }

    private void buildContent(ObjectStorageManagementView.FileItem file) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity section
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.FILE, I18n.t("sms.objects.details.field.name"), file.getName(), true);
        addFieldToGrid(identityGrid, VaadinIcon.PICTURE, I18n.t("sms.objects.details.field.type"), file.getType());
        addFieldToGrid(identityGrid, VaadinIcon.HARDDRIVE, I18n.t("sms.objects.details.field.size"), file.getSizeDisplay());
        addFieldToGrid(identityGrid, VaadinIcon.CALENDAR, I18n.t("sms.objects.details.field.modified"),
                file.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        mainLayout.add(createSection(I18n.t("sms.objects.details.section.identity"), identityGrid));

        // Metadata section
        Div metaGrid = createDetailGrid();
        addFieldToGrid(metaGrid, VaadinIcon.TAGS, I18n.t("sms.objects.details.field.tags"),
                file.getTags().isEmpty() ? I18n.t("sms.objects.details.field.tags.none") : String.join(", ", file.getTags()));
        addFieldToGrid(metaGrid, VaadinIcon.CODE, I18n.t("sms.objects.details.field.etag"), file.getEtag());
        addFieldToGrid(metaGrid, VaadinIcon.KEY, I18n.t("sms.objects.details.field.version"), file.getVersionID());
        addFieldToGrid(metaGrid, VaadinIcon.CHECK_CIRCLE, I18n.t("sms.objects.details.field.current.version"),
                file.isCurrentVersion() ? I18n.t("sms.objects.details.field.current.version.yes") : I18n.t("sms.objects.details.field.current.version.no"));
        mainLayout.add(createSection(I18n.t("sms.objects.details.section.metadata"), metaGrid));

        add(mainLayout);
    }
}