package eu.isygoit.ui.kms.views.secrets.digest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.secrets.digest.dialog.DeleteDigestConfigDialog;
import eu.isygoit.ui.kms.views.secrets.digest.dialog.DigestConfigDetailsDialog;
import eu.isygoit.ui.kms.views.secrets.digest.dialog.UpdateDigestConfigDialog;

import java.util.List;

public class DigestConfigCard extends BaseCard<DigestConfigView, DigestConfigService> {

    private final Runnable onDeleteRefresh;
    private DigestConfigDto dto;
    private Span titleSpan;

    public DigestConfigCard(DigestConfigView parentView,
                            DigestConfigService configService,
                            DigestConfigDto dto,
                            Runnable onDeleteRefresh) {
        super(parentView, configService);
        this.dto = dto;
        this.onDeleteRefresh = onDeleteRefresh;
        initCard();
    }

    public DigestConfigDto getDto() {
        return dto;
    }

    public void updateDto(DigestConfigDto newDto) {
        this.dto = newDto;
        refreshDisplay();
    }

    @Override
    protected String cardCssClassName() {
        return "digest-config-card";
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
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("kms.digest.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new DigestConfigDetailsDialog(dto).open());
        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("kms.digest.card.edit.tooltip"));
        editBtn.addClickListener(e -> openEditDialog());
        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, I18n.t("kms.digest.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteDigestConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.COG, I18n.t("kms.digest.card.algorithm"), dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : "—"));
        add(createIconRow(VaadinIcon.ROTATE_RIGHT, I18n.t("kms.digest.card.iterations"), String.valueOf(dto.getIterations())));
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
        new UpdateDigestConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }
}