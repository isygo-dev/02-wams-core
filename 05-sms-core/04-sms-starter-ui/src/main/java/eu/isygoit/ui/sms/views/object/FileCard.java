package eu.isygoit.ui.sms.views.object;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.sms.views.object.dialog.FileTagsDialog;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class FileCard extends BaseCard<ObjectStorageManagementView, ObjectStorageService> {

    private final ObjectStorageManagementView.FileItem file;
    private final String bucketName;
    private final Runnable onRefresh;

    public FileCard(ObjectStorageManagementView parentView, ObjectStorageService service,
                    ObjectStorageManagementView.FileItem file, String bucketName, Runnable onRefresh) {
        super(parentView, service);
        this.file = file;
        this.bucketName = bucketName;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override protected String cardCssClassName() { return "file-card"; }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        String displayName = file.getName() != null ? file.getName() : I18n.t("sms.objects.card.default.name");
        Span titleSpan = buildTitleSpan(displayName, file.getType());
        Span sizeChip = buildStatusChip(file.getSizeDisplay(), file.getSizeDisplay());
        titleLayout.add(titleSpan, sizeChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("sms.objects.action.details"),
                () -> parentView.showFileDetails(file));

        Button downloadBtn = createDownloadButton(I18n.t("sms.objects.action.download"),
                () -> parentView.downloadFile(file));

        Button tagsBtn = createTagsButton(I18n.t("sms.objects.action.manage.tags"),
                () -> new FileTagsDialog(parentView, objectService, parentView.getSelectedTenant(),
                        bucketName, file, () -> { if (onRefresh != null) onRefresh.run(); }).open());

        Button deleteBtn = createDeleteButton(I18n.t("sms.objects.action.delete"),
                () -> parentView.deleteFile(file));

        return List.of(detailsBtn, downloadBtn, tagsBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("wams-body-rows");

        body.add(createIconRow(VaadinIcon.FILE, I18n.t("sms.objects.card.name"), file.getName()));
        body.add(createIconRow(VaadinIcon.PICTURE, I18n.t("sms.objects.card.type"), file.getType()));
        body.add(createIconRow(VaadinIcon.HARDDRIVE, I18n.t("sms.objects.card.size"), file.getSizeDisplay()));
        body.add(createIconRow(VaadinIcon.CALENDAR, I18n.t("sms.objects.card.modified"),
                file.getModifiedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));

        if (file.getTags() != null && !file.getTags().isEmpty()) {
            HorizontalLayout tagsRow = new HorizontalLayout();
            tagsRow.setAlignItems(FlexComponent.Alignment.CENTER);
            tagsRow.setSpacing(true);
            tagsRow.setWidthFull();
            tagsRow.addClassName("meta-row");

            com.vaadin.flow.component.icon.Icon iconComponent = VaadinIcon.TAGS.create();
            iconComponent.setSize("14px");
            iconComponent.addClassName("meta-row-icon");

            Span labelSpan = new Span(I18n.t("sms.objects.card.tags") + ":");
            labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
            labelSpan.addClassName("meta-row-label");

            HorizontalLayout tagsLayout = new HorizontalLayout();
            tagsLayout.setSpacing(true);
            tagsLayout.getStyle().set("flex-wrap", "wrap");
            file.getTags().forEach(tag -> {
                Span tagBadge = new Span(tag);
                tagBadge.addClassName("tag-badge");
                tagBadge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
                tagBadge.getStyle().set("color", "var(--lumo-primary-color)");
                tagBadge.getStyle().set("padding", "var(--lumo-space-xs) var(--lumo-space-s)");
                tagBadge.getStyle().set("border-radius", "var(--lumo-border-radius)");
                tagBadge.getStyle().set("font-size", "var(--lumo-font-size-xxs)");
                tagsLayout.add(tagBadge);
            });

            tagsRow.add(iconComponent, labelSpan, tagsLayout);
            body.add(tagsRow);
        }

        add(body);
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

    public Button createDetailsButton(String tooltip, Runnable action) {
        Button btn = new Button(VaadinIcon.EYE.create());
        btn.addClassName("wams-card-action-button");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> action.run());
        return btn;
    }

    private Button createDownloadButton(String tooltip, Runnable action) {
        Button btn = new Button(VaadinIcon.DOWNLOAD.create());
        btn.addClassName("wams-card-action-button");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> action.run());
        return btn;
    }

    private Button createTagsButton(String tooltip, Runnable action) {
        Button btn = new Button(VaadinIcon.TAGS.create());
        btn.addClassName("wams-card-action-button");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> action.run());
        return btn;
    }

    @Override protected void onCardAttach(AttachEvent event) {}
}