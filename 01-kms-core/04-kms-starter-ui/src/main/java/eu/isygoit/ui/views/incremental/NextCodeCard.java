package eu.isygoit.ui.views.incremental;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.BaseCard;
import eu.isygoit.ui.views.incremental.dialog.DeleteNextCodeDialog;

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

    public NextCodeDto getDto() {
        return dto;
    }

    /**
     * Updates the card with new DTO data (called after generation or refresh).
     */
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
        deleteButton.addClickListener(e -> {
            new DeleteNextCodeDialog(objectService, dto.getId(),
                    dto.getEntity(), dto.getAttribute(),
                    deleteCallback).open();
        });

        return List.of(generateButton, deleteButton);
    }

    @Override
    protected void buildBodyRows() {
        // Formatted code preview
        Div codePreview = new Div();
        codePreview.addClassName(LumoUtility.Display.FLEX);
        codePreview.addClassName(LumoUtility.AlignItems.CENTER);
        codePreview.getStyle()
                .set("gap", "var(--lumo-space-s)")
                .set("margin-top", "var(--lumo-space-s)")
                .set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        Span previewLabel = new Span("Next code:");
        previewLabel.addClassName(LumoUtility.FontSize.SMALL);
        previewLabel.addClassName(LumoUtility.TextColor.SECONDARY);

        formattedCodeSpan = new Span();
        formattedCodeSpan.addClassName(LumoUtility.FontSize.LARGE);
        formattedCodeSpan.addClassName(LumoUtility.FontWeight.BOLD);
        formattedCodeSpan.getStyle().set("font-family", "monospace");
        updateFormattedCodeDisplay();

        Button copyFormattedBtn = MainView.createCopyButton(VaadinIcon.COPY, dto.getCode(), "Copy formatted code");
        codePreview.add(previewLabel, formattedCodeSpan, copyFormattedBtn);
        add(codePreview);

        // Details grid
        Div detailsGrid = new Div();
        detailsGrid.addClassName(LumoUtility.Display.GRID);
        detailsGrid.getStyle()
                .set("grid-template-columns", "repeat(2, 1fr)")
                .set("gap", "var(--lumo-space-s)")
                .set("margin-top", "var(--lumo-space-s)");

        addDetailPair(detailsGrid, "Prefix", dto.getPrefix() != null ? dto.getPrefix() : "—");
        addDetailPair(detailsGrid, "Suffix", dto.getSuffix() != null ? dto.getSuffix() : "—");
        addDetailPair(detailsGrid, "Value Length", String.valueOf(dto.getValueLength() != null ? dto.getValueLength() : 6));
        addDetailPair(detailsGrid, "Increment", String.valueOf(dto.getIncrement()));

        // Current numeric value row (span both columns)
        HorizontalLayout currentRow = new HorizontalLayout();
        currentRow.setAlignItems(FlexComponent.Alignment.CENTER);
        currentRow.setSpacing(true);
        Span currentLabel = new Span("Current numeric value:");
        currentLabel.addClassName(LumoUtility.FontSize.SMALL);
        currentLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        codeValueSpan = new Span();
        codeValueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        codeValueSpan.getStyle().set("font-family", "monospace");
        updateCodeValueDisplay();
        Button copyCodeBtn = MainView.createCopyButton(VaadinIcon.COPY, String.valueOf(dto.getCodeValue()), "Copy numeric value");
        currentRow.add(currentLabel, codeValueSpan, copyCodeBtn);
        currentRow.addClassName("full-width-row");
        detailsGrid.add(currentRow);

        add(detailsGrid);

        // Inject CSS for full-width row
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = '.full-width-row { grid-column: span 2; }'; document.head.appendChild(style);"
        ));
    }

    private void addDetailPair(Div container, String label, String value) {
        Div pair = new Div();
        pair.addClassName(LumoUtility.Display.FLEX);
        pair.addClassName(LumoUtility.JustifyContent.BETWEEN);
        pair.addClassName(LumoUtility.AlignItems.CENTER);
        pair.getStyle().set("gap", "var(--lumo-space-xs)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);
        labelSpan.addClassName(LumoUtility.TextColor.SECONDARY);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName(LumoUtility.FontWeight.MEDIUM);

        pair.add(labelSpan, valueSpan);
        container.add(pair);
    }

    private void generateNextCode() {
        generateButton.setEnabled(false);
        try {
            String generated = generateCallback.apply(dto.getEntity(), dto.getAttribute());
            Notification.show("Generated code: " + generated, 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // Refresh only this card, preserving page order
            parentView.refreshCard(this);
        } catch (Exception e) {
            Notification.show("Error generating code: " + e.getMessage(), 5000,
                            Notification.Position.BOTTOM_END)
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
}