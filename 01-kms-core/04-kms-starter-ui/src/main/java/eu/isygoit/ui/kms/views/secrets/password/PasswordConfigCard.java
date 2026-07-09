package eu.isygoit.ui.kms.views.secrets.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.secrets.password.dialog.DeletePasswordConfigDialog;
import eu.isygoit.ui.kms.views.secrets.password.dialog.PasswordConfigDetailsDialog;
import eu.isygoit.ui.kms.views.secrets.password.dialog.UpdatePasswordConfigDialog;

import java.util.List;

public class PasswordConfigCard extends BaseCard<PasswordConfigView, PasswordConfigService> {

    private final Runnable onDeleteRefresh;
    private PasswordConfigDto dto;
    private Span titleSpan;

    public PasswordConfigCard(PasswordConfigView parentView,
                              PasswordConfigService configService,
                              PasswordConfigDto dto,
                              Runnable onDeleteRefresh) {
        super(parentView, configService);
        this.dto = dto;
        this.onDeleteRefresh = onDeleteRefresh;
        initCard();
    }

    public PasswordConfigDto getDto() {
        return dto;
    }

    public void updateDto(PasswordConfigDto newDto) {
        this.dto = newDto;
        refreshDisplay();
    }

    @Override
    protected String cardCssClassName() {
        return "password-config-card";
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
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("kms.password.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new PasswordConfigDetailsDialog(dto).open());
        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("kms.password.card.edit.tooltip"));
        editBtn.addClickListener(e -> openEditDialog());
        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, I18n.t("kms.password.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeletePasswordConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.USER, I18n.t("kms.password.card.type"), dto.getType() != null ? dto.getType().meaning() : "—"));
        add(createIconRow(VaadinIcon.ARROW_DOWN, I18n.t("kms.password.card.min.length"), String.valueOf(dto.getMinLength())));
        add(createIconRow(VaadinIcon.ARROW_UP, I18n.t("kms.password.card.max.length"), String.valueOf(dto.getMaxLength())));
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
        new UpdatePasswordConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }
}