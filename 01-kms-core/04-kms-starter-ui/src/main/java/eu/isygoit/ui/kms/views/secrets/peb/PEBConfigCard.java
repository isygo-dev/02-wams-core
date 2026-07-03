package eu.isygoit.ui.kms.views.secrets.peb;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PEBConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PEBConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.secrets.peb.dialog.DeletePEBConfigDialog;
import eu.isygoit.ui.kms.views.secrets.peb.dialog.UpdatePEBConfigDialog;

import java.util.List;

public class PEBConfigCard extends BaseCard<PEBConfigView, PEBConfigService> {

    private final Runnable onDeleteRefresh;
    private PEBConfigDto dto;
    private Span titleSpan;

    public PEBConfigCard(PEBConfigView parentView,
                         PEBConfigService configService,
                         PEBConfigDto dto,
                         Runnable onDeleteRefresh) {
        super(parentView, configService);
        this.dto = dto;
        this.onDeleteRefresh = onDeleteRefresh;
        initCard();
    }

    public PEBConfigDto getDto() {
        return dto;
    }

    public void updateDto(PEBConfigDto newDto) {
        this.dto = newDto;
        refreshDisplay();
    }

    @Override
    protected String cardCssClassName() {
        return "peb-config-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.addClassName("card-title-group");
        titleSpan = buildTitleSpan(dto.getCode(), dto.getCode());
        left.add(titleSpan);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("kms.peb.card.edit.tooltip"));
        editBtn.addClickListener(e -> openEditDialog());
        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, I18n.t("kms.peb.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeletePEBConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.COG, I18n.t("kms.peb.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : "—"));
        add(createIconRow(VaadinIcon.ROTATE_RIGHT, I18n.t("kms.peb.card.iterations"), String.valueOf(dto.getKeyObtentionIterations())));
        add(createIconRow(VaadinIcon.DROP, I18n.t("kms.peb.card.salt.generator"), dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : "—"));
        add(createIconRow(VaadinIcon.RANDOM, I18n.t("kms.peb.card.iv.generator"), dto.getIvGenerator() != null ? dto.getIvGenerator().meaning() : "—"));
        add(createIconRow(VaadinIcon.UPLOAD, I18n.t("kms.peb.card.output.type"), dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : "—"));
        add(createIconRow(VaadinIcon.SERVER, I18n.t("kms.peb.card.provider"), dto.getProviderName() != null ? dto.getProviderName() : "—"));
        add(createIconRow(VaadinIcon.GROUP, I18n.t("kms.peb.card.pool.size"), String.valueOf(dto.getPoolSize())));
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("meta-row-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("meta-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void refreshDisplay() {
        titleSpan.setText(dto.getCode());
        titleSpan.getElement().setAttribute("title", dto.getCode());
        getUI().ifPresent(ui -> ui.access(() -> {
            List<Component> children = new java.util.ArrayList<>(getChildren().toList());
            boolean afterHeader = false;
            for (Component child : children) {
                if (child == headerRow) {
                    afterHeader = true;
                    continue;
                }
                if (afterHeader) remove(child);
            }
            buildBodyRows();
        }));
    }

    private void openEditDialog() {
        new UpdatePEBConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }
}