package eu.isygoit.ui.views.kms.secrets.digest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.DigestConfigDto;
import eu.isygoit.remote.kms.DigestConfigService;
import eu.isygoit.ui.views.common.card.BaseCard;
import eu.isygoit.ui.views.kms.secrets.digest.dialog.DeleteDigestConfigDialog;
import eu.isygoit.ui.views.kms.secrets.digest.dialog.UpdateDigestConfigDialog;

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
        deleteBtn.addClickListener(e -> new DeleteDigestConfigDialog(objectService, dto.getId(), dto.getCode(), onDeleteRefresh).open());
        return List.of(editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.COG, "Algorithm", dto.getAlgorithm() != null ? dto.getAlgorithm().meaning() : "—"));
        add(createIconRow(VaadinIcon.ROTATE_RIGHT, "Iterations", String.valueOf(dto.getIterations())));
        add(createIconRow(VaadinIcon.DROP, "Salt size (bytes)", String.valueOf(dto.getSaltSizeBytes())));
        add(createIconRow(VaadinIcon.DROP, "Salt generator", dto.getSaltGenerator() != null ? dto.getSaltGenerator().meaning() : "—"));
        add(createIconRow(VaadinIcon.UPLOAD, "Output type", dto.getStringOutputType() != null ? dto.getStringOutputType().meaning() : "—"));
        add(createIconRow(VaadinIcon.SERVER, "Provider", dto.getProviderName() != null ? dto.getProviderName() : "—"));
        add(createIconRow(VaadinIcon.GROUP, "Pool size", String.valueOf(dto.getPoolSize())));
        add(createIconRow(VaadinIcon.FLIP_H, "Invert position of salt", booleanToCheckmark(dto.getInvertPositionOfSaltInMessageBeforeDigesting())));
        add(createIconRow(VaadinIcon.FLIP_H, "Invert plain salt in results", booleanToCheckmark(dto.getInvertPositionOfPlainSaltInEncryptionResults())));
        add(createIconRow(VaadinIcon.CHECK, "Lenient salt size check", booleanToCheckmark(dto.getUseLenientSaltSizeCheck())));
        add(createIconRow(VaadinIcon.FONT, "Ignore Unicode normalization", booleanToCheckmark(dto.getUnicodeNormalizationIgnored())));
        if (dto.getPrefix() != null && !dto.getPrefix().isEmpty()) {
            add(createIconRow(VaadinIcon.TEXT_INPUT, "Prefix", dto.getPrefix()));
        }
        if (dto.getSuffix() != null && !dto.getSuffix().isEmpty()) {
            add(createIconRow(VaadinIcon.TEXT_INPUT, "Suffix", dto.getSuffix()));
        }
    }

    private String booleanToCheckmark(Boolean value) {
        return Boolean.TRUE.equals(value) ? "YES" : "NO";
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
        labelSpan.getStyle().set("min-width", "120px");

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
        new UpdateDigestConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .digest-config-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .digest-config-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .digest-config-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .digest-config-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}