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
        deleteBtn.addClickListener(e -> new DeletePEBConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.COG, "Algorithm", dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : "—"));
        add(createIconRow(VaadinIcon.ROTATE_RIGHT, "Iterations", String.valueOf(dto.getKeyObtentionIterations())));
        add(createIconRow(VaadinIcon.DROP, "Salt generator", dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : "—"));
        add(createIconRow(VaadinIcon.RANDOM, "IV generator", dto.getIvGenerator() != null ? dto.getIvGenerator().meaning() : "—"));
        add(createIconRow(VaadinIcon.UPLOAD, "Output type", dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : "—"));
        add(createIconRow(VaadinIcon.SERVER, "Provider", dto.getProviderName() != null ? dto.getProviderName() : "—"));
        add(createIconRow(VaadinIcon.GROUP, "Pool size", String.valueOf(dto.getPoolSize())));
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
        new UpdatePEBConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .peb-config-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .peb-config-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .peb-config-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .peb-config-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}