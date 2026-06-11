package eu.isygoit.ui.views.kms.tokenizer.config;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.views.common.card.BaseCard;
import eu.isygoit.ui.views.kms.tokenizer.config.dialog.DeleteTokenConfigDialog;
import eu.isygoit.ui.views.kms.tokenizer.config.dialog.UpdateTokenConfigDialog;

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
        left.getStyle().set("flex-wrap", "wrap");

        titleSpan = buildTitleSpan(dto.getCode(), dto.getCode());

        typeChip = buildStatusChip(
                dto.getTokenType() != null ? dto.getTokenType().meaning() : "UNKNOWN",
                dto.getTokenType() != null ? dto.getTokenType().name() : "UNKNOWN"
        );

        left.add(titleSpan, typeChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit configuration");
        editBtn.addClickListener(e -> openEditDialog());

        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, "Delete configuration");
        deleteBtn.addClickListener(e -> new DeleteTokenConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());

        return List.of(editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.BUILDING, "Issuer", dto.getIssuer() != null ? dto.getIssuer() : "—"));
        add(createIconRow(VaadinIcon.GROUP, "Audience", formatAudienceList()));
        add(createIconRow(VaadinIcon.CODE, "Algorithm", dto.getSignatureAlgorithm() != null ? dto.getSignatureAlgorithm() : "—"));
        add(createIconRow(VaadinIcon.CLOCK, "Lifetime", formatLifetime(dto.getLifeTimeInMs())));
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private String formatAudienceList() {
        if (dto.getAudience() == null || dto.getAudience().isEmpty()) return "—";
        return String.join(", ", dto.getAudience());
    }

    private String formatLifetime(Integer lifeTimeInMs) {
        if (lifeTimeInMs == null || lifeTimeInMs <= 0) return "—";
        long seconds = lifeTimeInMs / 1000;
        if (seconds < 60) return seconds + " second" + (seconds != 1 ? "s" : "");
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minute" + (minutes != 1 ? "s" : "");
        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours != 1 ? "s" : "");
        long days = hours / 24;
        return days + " day" + (days != 1 ? "s" : "");
    }

    private void refreshDisplay() {
        titleSpan.setText(dto.getCode());
        titleSpan.getElement().setAttribute("title", dto.getCode());
        typeChip.setText(dto.getTokenType() != null ? dto.getTokenType().meaning() : "UNKNOWN");
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

    @Override
    protected String buildExtraStyles() {
        return """
                .token-config-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .token-config-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .token-config-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .token-config-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}