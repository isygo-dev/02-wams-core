package eu.isygoit.ui.sms.views.storageconfig;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.StorageConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.StorageConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.sms.views.storageconfig.dialog.DeleteStorageConfigDialog;
import eu.isygoit.ui.sms.views.storageconfig.dialog.StorageConfigDetailsDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class StorageConfigCard extends BaseCard<StorageConfigManagementView, StorageConfigService> {

    private final StorageConfigDto config;
    private final Runnable onRefresh;

    public StorageConfigCard(StorageConfigManagementView parentView,
                             StorageConfigService storageConfigService,
                             StorageConfigDto config,
                             Runnable onRefresh) {
        super(parentView, storageConfigService);
        this.config = config;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "storageconfig-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        String displayName = config.getUserName() != null ? config.getUserName() : I18n.t("sms.storageconfig.card.default.name", config.getId());
        Span titleSpan = buildTitleSpan(displayName, config.getUrl());

        String typeLabel = config.getType() != null ? config.getType().name() : I18n.t("sms.storageconfig.card.type.unknown");
        Span typeChip = buildStatusChip(typeLabel, typeLabel);

        titleLayout.add(titleSpan, typeChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("sms.storageconfig.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new StorageConfigDetailsDialog(parentView, objectService, config.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("sms.storageconfig.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateStorageConfigDialog(config, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("sms.storageconfig.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteStorageConfigDialog(parentView, objectService, config.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("wams-body-rows");

        body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("sms.storageconfig.card.tenant"), config.getTenant()));
        body.add(createIconRow(VaadinIcon.USER, I18n.t("sms.storageconfig.card.username"), config.getUserName()));
        body.add(createIconRow(VaadinIcon.LINK, I18n.t("sms.storageconfig.card.url"), config.getUrl()));
        body.add(createIconRow(VaadinIcon.KEY, I18n.t("sms.storageconfig.card.password"), "••••••••"));

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

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }
}