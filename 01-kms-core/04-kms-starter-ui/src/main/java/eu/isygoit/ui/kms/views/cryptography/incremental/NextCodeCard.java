package eu.isygoit.ui.kms.views.cryptography.incremental;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.DeleteNextCodeDialog;

import java.util.List;

public class NextCodeCard extends BaseCard<IncrementalKeyView, KmsAppNextCodeService> {

    private final Runnable deleteCallback;
    private final java.util.function.BiFunction<String, String, String> generateCallback;
    private NextCodeDto dto;
    private Span formattedCodeSpan;
    private Span codeValueSpan;
    private Button generateButton;

    public NextCodeCard(IncrementalKeyView parentView,
                        KmsAppNextCodeService nextCodeService,
                        NextCodeDto dto,
                        Runnable deleteCallback,
                        java.util.function.BiFunction<String, String, String> generateCallback) {
        super(parentView, nextCodeService);
        this.dto = dto;
        this.deleteCallback = deleteCallback;
        this.generateCallback = generateCallback;
        initCard();
    }

    // ✅ Public getter for the DTO (used by parent view's refreshCard method)
    public NextCodeDto getDto() {
        return dto;
    }

    public void updateDto(NextCodeDto newDto) {
        this.dto = newDto;
        refreshDisplay();
    }

    @Override
    protected String cardCssClassName() {
        return "next-code-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");
        Span titleSpan = buildTitleSpan(dto.getEntity() + " : " + dto.getAttribute(), null);
        left.add(titleSpan);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        generateButton = createIconButton(VaadinIcon.COG, "Generate next code");
        generateButton.addClickListener(e -> generateNextCode());

        Button deleteButton = createDangerIconButton(VaadinIcon.TRASH, "Delete configuration");
        deleteButton.addClickListener(e -> new DeleteNextCodeDialog(objectService, dto.getId(),
                dto.getEntity(), dto.getAttribute(), deleteCallback).open());

        return List.of(generateButton, deleteButton);
    }

    @Override
    protected void buildBodyRows() {
        // Formatted code preview
        HorizontalLayout codePreviewRow = new HorizontalLayout();
        codePreviewRow.setAlignItems(FlexComponent.Alignment.CENTER);
        codePreviewRow.setSpacing(true);
        codePreviewRow.setWidthFull();
        codePreviewRow.getStyle().set("margin-top", "var(--lumo-space-xs)");
        codePreviewRow.addClassName("meta-row");

        Icon codeIcon = VaadinIcon.CODE.create();
        codeIcon.setSize("16px");
        codeIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span codeLabel = new Span("Next code:");
        codeLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        codeLabel.addClassName(LumoUtility.FontSize.XSMALL);
        codeLabel.getStyle().set("min-width", "100px");

        formattedCodeSpan = new Span();
        formattedCodeSpan.addClassName(LumoUtility.FontSize.LARGE);
        formattedCodeSpan.addClassName(LumoUtility.FontWeight.BOLD);
        formattedCodeSpan.getStyle().set("font-family", "monospace");
        updateFormattedCodeDisplay();

        Button copyFormattedBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, dto.getCode(), "Copy formatted code");
        copyFormattedBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        codePreviewRow.add(codeIcon, codeLabel, formattedCodeSpan, copyFormattedBtn);
        codePreviewRow.expand(formattedCodeSpan);
        add(codePreviewRow);

        // Details as icon rows
        add(createIconRow(VaadinIcon.FILE_TEXT, "Entity", dto.getEntity()));
        add(createIconRow(VaadinIcon.TAG, "Attribute", dto.getAttribute()));
        add(createIconRow(VaadinIcon.ALIGN_LEFT, "Prefix", dto.getPrefix() != null ? dto.getPrefix() : "—"));
        add(createIconRow(VaadinIcon.ALIGN_RIGHT, "Suffix", dto.getSuffix() != null ? dto.getSuffix() : "—"));
        add(createIconRow(VaadinIcon.HASH, "Value length", String.valueOf(dto.getValueLength() != null ? dto.getValueLength() : 6)));
        add(createIconRow(VaadinIcon.ARROW_UP, "Increment", String.valueOf(dto.getIncrement())));

        // Current numeric value row (with copy)
        HorizontalLayout currentRow = new HorizontalLayout();
        currentRow.setAlignItems(FlexComponent.Alignment.CENTER);
        currentRow.setSpacing(true);
        currentRow.setWidthFull();
        currentRow.getStyle().set("margin-top", "var(--lumo-space-xs)");
        currentRow.addClassName("meta-row");

        Icon currentIcon = VaadinIcon.CALC.create();
        currentIcon.setSize("16px");
        currentIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span currentLabel = new Span("Current value:");
        currentLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        currentLabel.addClassName(LumoUtility.FontSize.XSMALL);
        currentLabel.getStyle().set("min-width", "100px");

        codeValueSpan = new Span();
        codeValueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        codeValueSpan.getStyle().set("font-family", "monospace");
        updateCodeValueDisplay();

        Button copyCurrentBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, String.valueOf(dto.getCodeValue()), "Copy numeric value");
        copyCurrentBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        currentRow.add(currentIcon, currentLabel, codeValueSpan, copyCurrentBtn);
        currentRow.expand(codeValueSpan);
        add(currentRow);
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

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void generateNextCode() {
        generateButton.setEnabled(false);
        try {
            String generated = generateCallback.apply(dto.getEntity(), dto.getAttribute());
            Notification.show("Generated code: " + generated, 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            parentView.refreshCard(this);
        } catch (Exception e) {
            Notification.show("Error generating code: " + e.getMessage(), 5000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            generateButton.setEnabled(true);
        }
    }

    private void updateFormattedCodeDisplay() {
        formattedCodeSpan.setText(dto.getCode());
    }

    private void updateCodeValueDisplay() {
        long val = dto.getCodeValue() != null ? dto.getCodeValue() : 0L;
        codeValueSpan.setText(String.valueOf(val));
    }

    private void refreshDisplay() {
        updateFormattedCodeDisplay();
        updateCodeValueDisplay();
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .next-code-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .next-code-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .next-code-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .next-code-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}