package eu.isygoit.ui.sms.views.bucket;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.sms.views.bucket.dialog.BucketDetailsDialog;
import eu.isygoit.ui.sms.views.bucket.dialog.DeleteBucketDialog;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class BucketCard extends BaseCard<BucketManagementView, ObjectStorageService> {

    private final BucketDto bucket;
    private final Runnable onRefresh;

    public BucketCard(BucketManagementView parentView, ObjectStorageService service, BucketDto bucket, Runnable onRefresh) {
        super(parentView, service);
        this.bucket = bucket;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override protected String cardCssClassName() { return "bucket-card"; }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        String displayName = bucket.getName() != null ? bucket.getName() : I18n.t("sms.buckets.card.default.name");
        Span titleSpan = buildTitleSpan(displayName, null);

        String created = bucket.getCreationDate() != null ?
                bucket.getCreationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) :
                I18n.t("sms.buckets.card.date.unknown");
        Span dateChip = buildStatusChip(created, created);
        titleLayout.add(titleSpan, dateChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("sms.buckets.action.details"),
                () -> new BucketDetailsDialog(parentView, objectService, parentView.getSelectedTenant(), bucket).open());
        Button browseBtn = createBrowseButton(I18n.t("sms.buckets.action.browse"),
                () -> parentView.navigateToObjectStorage(parentView.getSelectedTenant(), bucket));
        Button deleteBtn = createDeleteButton(I18n.t("sms.buckets.action.delete"),
                () -> new DeleteBucketDialog(parentView, objectService, parentView.getSelectedTenant(), bucket.getName(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());
        return List.of(detailsBtn, browseBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("wams-body-rows");

        body.add(createIconRow(VaadinIcon.FOLDER, I18n.t("sms.buckets.card.name"), bucket.getName()));
        body.add(createIconRow(VaadinIcon.CALENDAR, I18n.t("sms.buckets.card.created"),
                bucket.getCreationDate() != null ? bucket.getCreationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null));

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

    private Button createBrowseButton(String tooltip, Runnable action) {
        Button btn = new Button(VaadinIcon.FOLDER_OPEN.create());
        btn.addClassName("wams-card-action-button");
        btn.setTooltipText(tooltip);
        btn.addClickListener(e -> action.run());
        return btn;
    }

    @Override protected void onCardAttach(AttachEvent event) {}
}