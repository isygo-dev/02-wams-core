package eu.isygoit.ui.kms.views.secrets.password;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.secrets.password.dialog.DeletePasswordConfigDialog;
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
        left.getStyle().set("flex-wrap", "wrap");
        titleSpan = buildTitleSpan(dto.getCode(), dto.getCode());
        left.add(titleSpan);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit configuration");
        editBtn.addClickListener(e -> openEditDialog());
        Button deleteBtn = createDangerIconButton(VaadinIcon.TRASH, "Delete configuration");
        deleteBtn.addClickListener(e -> new DeletePasswordConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.USER, "Type", dto.getType() != null ? dto.getType().meaning() : "—"));
        add(createIconRow(VaadinIcon.TEXT_INPUT, "Pattern", dto.getPattern() != null ? dto.getPattern() : "—"));
        add(createIconRow(VaadinIcon.FONT, "Character set", dto.getCharSetType() != null ? dto.getCharSetType().meaning() : "—"));
        add(createIconRow(VaadinIcon.ARROW_DOWN, "Min length", String.valueOf(dto.getMinLength())));
        add(createIconRow(VaadinIcon.ARROW_UP, "Max length", String.valueOf(dto.getMaxLength())));
        add(createIconRow(VaadinIcon.CLOCK, "Lifetime (days)", String.valueOf(dto.getLifeTime())));
        if (dto.getInitial() != null && !dto.getInitial().isEmpty()) {
            add(createIconRow(VaadinIcon.FLAG, "Initial value", dto.getInitial()));
        }
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
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

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

    @Override
    protected String buildExtraStyles() {
        return """
                .password-config-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .password-config-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .password-config-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .password-config-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}