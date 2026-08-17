package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.s3.object.MetaData;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
public class FileDetailsDialog extends DetailsViewDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final ObjectStorageManagementView.FileItem file;
    private final VerticalLayout metadataLayout = new VerticalLayout();

    public FileDetailsDialog(ObjectStorageManagementView parentView,
                             ObjectStorageService objectStorageService,
                             String tenant,
                             String bucketName,
                             ObjectStorageManagementView.FileItem file) {
        super(I18n.t("sms.objects.details.title", file.getFileName()));
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

        buildContent();
    }

    private void buildContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.FILE, I18n.t("sms.objects.details.field.name"), file.getFileName(), true);
        addFieldToGrid(identityGrid, VaadinIcon.FOLDER_O, I18n.t("sms.objects.details.field.path"),
                file.getPath() != null && !file.getPath().isBlank() ? file.getPath() : I18n.t("sms.objects.card.path.root"));
        addFieldToGrid(identityGrid, VaadinIcon.PICTURE, I18n.t("sms.objects.details.field.type"), file.getType());
        addFieldToGrid(identityGrid, VaadinIcon.HARDDRIVE, I18n.t("sms.objects.details.field.size"), file.getSizeDisplay());
        addFieldToGrid(identityGrid, VaadinIcon.CALENDAR, I18n.t("sms.objects.details.field.modified"),
                file.getModifiedDate() != null ? file.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        mainLayout.add(createSection(I18n.t("sms.objects.details.section.identity"), identityGrid));

        metadataLayout.setPadding(false);
        metadataLayout.setSpacing(true);
        metadataLayout.add(new Span(I18n.t("sms.objects.details.loading.metadata")));
        mainLayout.add(createSection(I18n.t("sms.objects.details.section.metadata"), metadataLayout));

        // NoActionDialog already renders its own Close button in the real
        // dialog footer — this row only needs the metadata-refresh action.
        HorizontalLayout actionsRow = new HorizontalLayout();
        actionsRow.setSpacing(true);
        actionsRow.setWidthFull();
        actionsRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button refreshMetaBtn = new Button(I18n.t("sms.objects.details.refresh.metadata"), VaadinIcon.REFRESH.create());
        refreshMetaBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshMetaBtn.addClickListener(e -> loadMetadata());

        actionsRow.add(refreshMetaBtn);
        mainLayout.add(actionsRow);

        add(mainLayout);

        addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                loadMetadata();
            }
        });
    }

    private void loadMetadata() {
        metadataLayout.removeAll();
        metadataLayout.add(new Span(I18n.t("sms.objects.details.loading.metadata")));

        try {
            ResponseEntity<MetaData> response = objectStorageService.getMetadata(
                    tenant, bucketName, file.getPath(), file.getFileName(), file.getVersionID());
            MetaData meta = response.getBody();
            metadataLayout.removeAll();
            if (meta != null) {
                displayMetadata(meta);
            } else {
                metadataLayout.add(new Span(I18n.t("sms.objects.details.no.metadata")));
            }
        } catch (FeignException ex) {
            metadataLayout.removeAll();
            metadataLayout.add(new Span(I18n.t("sms.objects.details.metadata.error", extractErrorMessage(ex))));
            log.error("Failed to fetch metadata for {}", file.getName(), ex);
        } catch (Exception e) {
            metadataLayout.removeAll();
            metadataLayout.add(new Span(I18n.t("sms.objects.details.metadata.error", e.getMessage())));
            log.error("Failed to fetch metadata for {}", file.getName(), e);
        }
    }

    private void displayMetadata(MetaData meta) {
        Div grid = createDetailGrid();

        addFieldToGrid(grid, VaadinIcon.FILE_O, I18n.t("sms.objects.details.field.content.type"), meta.getContentType());
        addFieldToGrid(grid, VaadinIcon.HARDDRIVE, I18n.t("sms.objects.details.field.size"),
                meta.getSize() != 0 ? formatSize(meta.getSize()) : null);
        addFieldToGrid(grid, VaadinIcon.CODE, I18n.t("sms.objects.details.field.etag"), meta.getEtag(), true);
        addFieldToGrid(grid, VaadinIcon.CHILD, I18n.t("sms.objects.details.field.version"), meta.getVersionID(), true);
        addFieldToGrid(grid, VaadinIcon.CALENDAR, I18n.t("sms.objects.details.field.last.modified"), meta.getLastModified());

        if (meta.getTags() != null && !meta.getTags().isEmpty()) {
            addFieldToGrid(grid, VaadinIcon.TAGS, I18n.t("sms.objects.details.field.tags"),
                    String.join(", ", meta.getTags()));
        }
        if (meta.getTagsMap() != null && !meta.getTagsMap().isEmpty()) {
            String mapStr = meta.getTagsMap().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", "));
            addFieldToGrid(grid, VaadinIcon.TAG, I18n.t("sms.objects.details.field.tags.map"), mapStr);
        }

        metadataLayout.add(grid);
    }

    private String formatSize(Long size) {
        if (size == null) return null;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) {
                return ex.contentUTF8();
            }
        } catch (Exception ignored) {
        }
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}
