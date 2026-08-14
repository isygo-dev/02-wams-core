package eu.isygoit.ui.dms.views.linkedFile;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.LinkedFileResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.LinkedFileService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.dms.views.linkedFile.dialog.DeleteLinkedFileDialog;
import eu.isygoit.ui.dms.views.linkedFile.dialog.FileDetailsViewDialog;
import eu.isygoit.ui.dms.views.linkedFile.dialog.RenameLinkedFileDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class LinkedFileCard extends BaseCard<LinkedFileManagementView, LinkedFileService> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private LinkedFileResponseDto file;
    private final Runnable onRefresh;

    public LinkedFileCard(LinkedFileManagementView parentView,
                          LinkedFileService linkedFileService,
                          LinkedFileResponseDto file,
                          Runnable onRefresh) {
        super(parentView, linkedFileService);
        this.file = file;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "linked-file-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        // File name with icon
        HorizontalLayout nameLayout = new HorizontalLayout();
        nameLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        nameLayout.setSpacing(true);

        VaadinIcon fileIcon = getFileIcon(file.getOriginalFileName());
        com.vaadin.flow.component.icon.Icon iconComponent = fileIcon.create();
        iconComponent.setSize("20px");
        iconComponent.addClassName("file-type-icon");

        Span titleSpan = new Span(file.getOriginalFileName());
        titleSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);

        nameLayout.add(iconComponent, titleSpan);

        // Add code chip
        Span codeChip = buildStatusChip(
                I18n.t("dms.linkedfile.card.code.chip", file.getCode()),
                BaseCard.ChipColor.INFO
        );

        titleLayout.add(nameLayout, codeChip);
        return titleLayout;
    }

    private VaadinIcon getFileIcon(String fileName) {
        if (fileName == null) return VaadinIcon.FILE;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return VaadinIcon.FILE_O;
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return VaadinIcon.FILE_TEXT;
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) return VaadinIcon.FILE_TABLE;
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) return VaadinIcon.FILE_PRESENTATION;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".svg")) {
            return VaadinIcon.FILE_PICTURE;
        }
        if (lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z")) return VaadinIcon.FILE_ZIP;
        if (lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov")) return VaadinIcon.FILE_MOVIE;
        if (lower.endsWith(".mp3") || lower.endsWith(".wav")) return VaadinIcon.FILE_SOUND;
        return VaadinIcon.FILE;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("dms.linkedfile.card.details.tooltip"),
                () -> new FileDetailsViewDialog(file).open());

        Button downloadBtn = createIconButton(VaadinIcon.DOWNLOAD, I18n.t("dms.linkedfile.card.download.tooltip"));
        downloadBtn.addClickListener(e -> downloadFile());

        Button renameBtn = createEditButton(I18n.t("dms.linkedfile.card.rename.tooltip"),
                () -> new RenameLinkedFileDialog(parentView, objectService, file, renamed -> {
                    file = renamed;
                    if (onRefresh != null) onRefresh.run();
                }).open());

        Button deleteBtn = createDeleteButton(I18n.t("dms.linkedfile.card.delete.tooltip"),
                () -> new DeleteLinkedFileDialog(
                        parentView,
                        objectService,
                        file.getCode(),
                        file.getOriginalFileName(), () -> {if (onRefresh != null) onRefresh.run();}
                ).open());

        return List.of(detailsBtn, downloadBtn, renameBtn, deleteBtn);
    }

    private void downloadFile() {
        parentView.showLoading(true);
        try {
            ResponseEntity<Resource> response = objectService.download(file.getCode());
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                Notification.show(I18n.t("dms.linkedfile.card.download.error"), 5000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            byte[] content = response.getBody().getInputStream().readAllBytes();
            String downloadName = file.getOriginalFileName() != null ? file.getOriginalFileName() : file.getCode();
            StreamResource streamResource = new StreamResource(downloadName, () -> new ByteArrayInputStream(content));

            Anchor downloadAnchor = new Anchor(streamResource, "");
            downloadAnchor.getElement().setAttribute("download", true);
            downloadAnchor.getStyle().set("display", "none");
            add(downloadAnchor);
            downloadAnchor.getElement().executeJs("this.click(); this.remove();");
        } catch (FeignException ex) {
            log.error("Download failed", ex);
            Notification.show(I18n.t("dms.linkedfile.card.download.error"), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (IOException ex) {
            log.error("Failed to read downloaded file content", ex);
            Notification.show(I18n.t("dms.linkedfile.card.download.error"), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            parentView.showLoading(false);
        }
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("wams-body-rows");

        // Path row
        if (file.getPath() != null && !file.getPath().isEmpty()) {
            body.add(createIconRow(VaadinIcon.FOLDER, I18n.t("dms.linkedfile.card.path"), file.getPath()));
        }

        // Tags row
        if (file.getTags() != null && !file.getTags().isEmpty()) {
            body.add(createTagsRow(file.getTags()));
        }

        // Categories row
        if (file.getCategoryNames() != null && !file.getCategoryNames().isEmpty()) {
            body.add(createCategoriesRow(file.getCategoryNames()));
        }

        // Tenant row
        if (file.getTenant() != null && !file.getTenant().isEmpty()) {
            body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("dms.linkedfile.card.tenant"), file.getTenant()));
        }

        add(body);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DATE_FORMATTER);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.addClassName("meta-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
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
        iconComponent.setSize("14px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(I18n.t("dms.linkedfile.card.tags") + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.addClassName("meta-row-label");

        HorizontalLayout tagsContainer = new HorizontalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.getStyle().set("flex-wrap", "wrap");
        tagsContainer.getStyle().set("gap", "4px");

        for (String tag : tags) {
            Span tagChip = new Span(tag);
            tagChip.addClassName("wams-tag-chip");
            tagChip.addClassName(LumoUtility.FontSize.XXSMALL);
            tagsContainer.add(tagChip);
        }

        row.add(iconComponent, labelSpan, tagsContainer);
        row.expand(tagsContainer);
        return row;
    }

    private HorizontalLayout createCategoriesRow(List<String> categories) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = VaadinIcon.LIST.create();
        iconComponent.setSize("14px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(I18n.t("dms.linkedfile.card.categories") + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.addClassName("meta-row-label");

        HorizontalLayout categoriesContainer = new HorizontalLayout();
        categoriesContainer.setSpacing(true);
        categoriesContainer.getStyle().set("flex-wrap", "wrap");
        categoriesContainer.getStyle().set("gap", "4px");

        for (String category : categories) {
            Span categoryChip = new Span(category);
            categoryChip.addClassName("wams-category-chip");
            categoryChip.addClassName(LumoUtility.FontSize.XXSMALL);
            categoriesContainer.add(categoryChip);
        }

        row.add(iconComponent, labelSpan, categoriesContainer);
        row.expand(categoriesContainer);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }
}