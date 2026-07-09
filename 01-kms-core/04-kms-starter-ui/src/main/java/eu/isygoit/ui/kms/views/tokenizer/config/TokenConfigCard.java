package eu.isygoit.ui.kms.views.tokenizer.config;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.tokenizer.config.dialog.DeleteTokenConfigDialog;
import eu.isygoit.ui.kms.views.tokenizer.config.dialog.TokenConfigDetailsViewDialog;
import eu.isygoit.ui.kms.views.tokenizer.config.dialog.UpdateTokenConfigDialog;

import java.util.List;

public class TokenConfigCard extends BaseCard<TokenConfigView, KmsTokenConfigService> {

    private final KmsApiService kmsApiService;
    private final Runnable onDeleteRefresh;
    private TokenConfigDto dto;
    private Span titleSpan;
    private Span typeChip;

    public TokenConfigCard(TokenConfigView parentView,
                           KmsTokenConfigService tokenConfigService,
                           KmsApiService kmsApiService,
                           TokenConfigDto dto,
                           Runnable onDeleteRefresh) {
        super(parentView, tokenConfigService);
        this.kmsApiService = kmsApiService;
        this.dto = dto;
        this.onDeleteRefresh = onDeleteRefresh;
        initCard();
    }

    public TokenConfigDto getDto() {
        return dto;
    }

    public void updateDto(TokenConfigDto newDto) {
        this.dto = newDto;
        refreshDisplay();
    }

    @Override
    protected String cardCssClassName() {
        return "token-config-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.addClassName("card-title-group");

        titleSpan = buildTitleSpan(dto.getCode(), dto.getCode());

        typeChip = buildStatusChip(
                dto.getTokenType() != null ? dto.getTokenType().meaning() : I18n.t("kms.token.config.type.unknown"),
                dto.getTokenType() != null ? dto.getTokenType().name() : I18n.t("kms.token.config.type.unknown")
        );

        left.add(titleSpan, typeChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("kms.token.config.details.button"),
                () -> new TokenConfigDetailsViewDialog(dto).open());

        Button editBtn = createEditButton(I18n.t("kms.token.config.edit.button"), this::openEditDialog);

        Button deleteBtn = createDeleteButton(I18n.t("kms.token.config.delete.button"),
                () -> new DeleteTokenConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.CLOCK, I18n.t("kms.token.config.lifetime"), formatLifetime(dto.getLifeTimeInMs())));
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

    private String formatLifetime(Integer lifeTimeInMs) {
        if (lifeTimeInMs == null || lifeTimeInMs <= 0) return "—";
        long seconds = lifeTimeInMs / 1000;
        if (seconds < 60) return seconds + " " + I18n.t("time.seconds");
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " " + I18n.t("time.minutes");
        long hours = minutes / 60;
        if (hours < 24) return hours + " " + I18n.t("time.hours");
        long days = hours / 24;
        return days + " " + I18n.t("time.days");
    }

    private void refreshDisplay() {
        titleSpan.setText(dto.getCode());
        titleSpan.getElement().setAttribute("title", dto.getCode());
        typeChip.setText(dto.getTokenType() != null ? dto.getTokenType().meaning() : I18n.t("kms.token.config.type.unknown"));
        getUI().ifPresent(ui -> ui.access(() -> {
            List<Component> children = new java.util.ArrayList<>(getChildren().toList());
            boolean headerRemoved = false;
            for (Component child : children) {
                if (child == headerRow) {
                    headerRemoved = true;
                    continue;
                }
                if (headerRemoved) remove(child);
            }
            buildBodyRows();
        }));
    }

    private void openEditDialog() {
        new UpdateTokenConfigDialog(objectService, kmsApiService, dto, () -> parentView.refreshCard(this)).open();
    }
}