package eu.isygoit.ui.sms.views.object;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.sms.views.object.dialog.DeleteFileDialog;
import eu.isygoit.ui.sms.views.object.dialog.FileDetailsDialog;
import eu.isygoit.ui.sms.views.object.dialog.FileTagsDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class FileCard extends BaseCard<ObjectStorageManagementView, ObjectStorageService> {

    private final ObjectStorageManagementView.FileItem file;
    private final String tenant;
    private final String bucketName;
    private final Runnable onRefresh;

    public FileCard(ObjectStorageManagementView parentView, ObjectStorageService service,
                    ObjectStorageManagementView.FileItem file, String tenant, String bucketName, Runnable onRefresh) {
        super(parentView, service);
        this.file = file;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "file-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        // Just the base file name, not the full object key — a nested file's
        // path (e.g. "reports/2024/summary.pdf") belongs in a body row, not
        // the card's headline, same convention as every other file-bearing
        // card in the app (e.g. LinkedFileCard).
        Span titleSpan = buildTitleSpan(file.getFileName(), file.getName());
        Span typeChip = buildStatusChip(
                file.getType() != null && !file.getType().isBlank() ? file.getType().toUpperCase() : I18n.t("sms.objects.card.type.unknown"),
                BaseCard.ChipColor.INFO
        );
        titleLayout.add(titleSpan, typeChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("sms.objects.action.details"),
                () -> new FileDetailsDialog(parentView, objectService, tenant, bucketName, file).open());

        Button downloadBtn = createIconButton(VaadinIcon.DOWNLOAD, I18n.t("sms.objects.action.download"));
        downloadBtn.addClickListener(e -> downloadFile());

        Button tagsBtn = createIconButton(VaadinIcon.TAGS, I18n.t("sms.objects.action.manage.tags"));
        tagsBtn.addClickListener(e -> new FileTagsDialog(parentView, objectService, tenant, bucketName, file,
                () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        Button shareBtn = createIconButton(VaadinIcon.LINK, I18n.t("sms.objects.action.share.link"));
        shareBtn.addClickListener(e -> generatePresignedUrl());

        Button deleteBtn = createDeleteButton(I18n.t("sms.objects.action.delete"),
                () -> new DeleteFileDialog(parentView, objectService, tenant, bucketName, file,
                        () -> {
                            if (onRefresh != null) onRefresh.run();
                        }).open());

        return List.of(detailsBtn, downloadBtn, tagsBtn, shareBtn, deleteBtn);
    }

    private void downloadFile() {
        parentView.showLoading(true);
        try {
            ResponseEntity<Resource> response = objectService.download(
                    tenant, bucketName, file.getPath(), file.getFileName(), file.getVersionID());

            Resource resource = response.getBody();
            if (resource == null) {
                showError(I18n.t("sms.objects.download.error", "File not found"));
                return;
            }

            byte[] data = resource.getInputStream().readAllBytes();
            StreamResource streamResource = new StreamResource(file.getFileName(), () -> new ByteArrayInputStream(data));
            streamResource.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            Anchor anchor = new Anchor(streamResource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getStyle().set("display", "none");
            add(anchor);
            anchor.getElement().executeJs("this.click(); this.remove();");

            Notification.show(I18n.t("sms.objects.download.started", file.getFileName()), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (FeignException ex) {
            showError(I18n.t("sms.objects.download.error", extractErrorMessage(ex)));
            log.error("Download failed for {}", file.getName(), ex);
        } catch (IOException e) {
            showError(I18n.t("sms.objects.download.error", e.getMessage()));
            log.error("Download failed for {}", file.getName(), e);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void generatePresignedUrl() {
        try {
            ResponseEntity<String> response = objectService.getPresignedUrl(
                    tenant, bucketName, file.getPath(), file.getFileName());
            String url = response.getBody();
            if (url == null || url.isBlank()) {
                showError(I18n.t("sms.objects.presigned.url.empty"));
                return;
            }
            UI.getCurrent().getPage().open(url, "_blank");
        } catch (FeignException ex) {
            showError(I18n.t("sms.objects.presigned.url.error", extractErrorMessage(ex)));
            log.error("Failed to generate pre-signed URL for file: {}", file.getName(), ex);
        } catch (Exception e) {
            showError(I18n.t("sms.objects.presigned.url.error", e.getMessage()));
            log.error("Failed to generate pre-signed URL for file: {}", file.getName(), e);
        }
    }

    private void showError(String message) {
        Notification.show(message, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.FOLDER, I18n.t("sms.objects.card.path"),
                file.getPath() != null && !file.getPath().isBlank() ? file.getPath() : I18n.t("sms.objects.card.path.root")));
        add(createIconRow(VaadinIcon.HARDDRIVE, I18n.t("sms.objects.card.size"), file.getSizeDisplay()));
        add(createIconRow(VaadinIcon.CALENDAR, I18n.t("sms.objects.card.modified"),
                file.getModifiedDate() != null ? file.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null));

        if (file.getTags() != null && !file.getTags().isEmpty()) {
            add(createTagsRow(file.getTags()));
        }
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("meta-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("meta-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private HorizontalLayout createTagsRow(List<String> tags) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = VaadinIcon.TAGS.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(I18n.t("sms.objects.card.tags") + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("meta-row-label");

        HorizontalLayout tagsLayout = new HorizontalLayout();
        tagsLayout.setSpacing(true);
        tagsLayout.addClassName("wams-tags-wrap");
        for (String tag : tags) {
            tagsLayout.add(buildStatusChip(tag, BaseCard.ChipColor.INFO));
        }

        row.add(iconComponent, labelSpan, tagsLayout);
        row.expand(tagsLayout);
        return row;
    }
}
