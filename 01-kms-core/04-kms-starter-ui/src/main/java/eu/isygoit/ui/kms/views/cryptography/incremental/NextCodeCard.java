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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.NextCodeDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsAppNextCodeService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.DeleteNextCodeConfigDialog;
import eu.isygoit.ui.kms.views.cryptography.incremental.dialog.NextCodeDetailsDialog;

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
        left.addClassName("kms-partc-title-wrap");
        Span titleSpan = buildTitleSpan(dto.getEntity() + " : " + dto.getAttribute(), null);
        left.add(titleSpan);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        generateButton = createIconButton(VaadinIcon.COG, I18n.t("kms.nextcode.card.generate.tooltip"));
        generateButton.addClickListener(e -> generateNextCode());

        Button detailsButton = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("kms.nextcode.card.details.tooltip"));
        detailsButton.addClickListener(e -> new NextCodeDetailsDialog(dto).open());

        Button deleteButton = createDangerIconButton(VaadinIcon.TRASH, I18n.t("kms.nextcode.card.delete.tooltip"));
        deleteButton.addClickListener(e -> new DeleteNextCodeConfigDialog(objectService, dto.getId(),
                dto.getEntity(), dto.getAttribute(), deleteCallback).open());

        return List.of(generateButton, detailsButton, deleteButton);
    }

    @Override
    protected void buildBodyRows() {
        // Formatted code preview
        HorizontalLayout codePreviewRow = new HorizontalLayout();
        codePreviewRow.setAlignItems(FlexComponent.Alignment.CENTER);
        codePreviewRow.setSpacing(true);
        codePreviewRow.setWidthFull();
        codePreviewRow.addClassName("meta-row");

        Icon codeIcon = VaadinIcon.CODE.create();
        codeIcon.setSize("16px");
        codeIcon.addClassName("kms-partc-row-icon");

        Span codeLabel = new Span(I18n.t("kms.nextcode.card.next.code"));
        codeLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        codeLabel.addClassName(LumoUtility.FontSize.XSMALL);
        codeLabel.addClassName("kms-partc-row-label");

        formattedCodeSpan = new Span();
        formattedCodeSpan.addClassName(LumoUtility.FontSize.LARGE);
        formattedCodeSpan.addClassName(LumoUtility.FontWeight.BOLD);
        formattedCodeSpan.addClassName("kms-partc-row-value-mono");
        updateFormattedCodeDisplay();

        Button copyFormattedBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, dto.getCode(), I18n.t("kms.nextcode.card.copy.formatted"));
        copyFormattedBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        codePreviewRow.add(codeIcon, codeLabel, formattedCodeSpan, copyFormattedBtn);
        codePreviewRow.expand(formattedCodeSpan);
        add(codePreviewRow);

        // Details as icon rows
        add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("kms.nextcode.card.entity"), dto.getEntity()));
        add(createIconRow(VaadinIcon.TAG, I18n.t("kms.nextcode.card.attribute"), dto.getAttribute()));
        add(createIconRow(VaadinIcon.ALIGN_LEFT, I18n.t("kms.nextcode.card.prefix"), dto.getPrefix() != null ? dto.getPrefix() : "—"));
        add(createIconRow(VaadinIcon.ALIGN_RIGHT, I18n.t("kms.nextcode.card.suffix"), dto.getSuffix() != null ? dto.getSuffix() : "—"));
        add(createIconRow(VaadinIcon.HASH, I18n.t("kms.nextcode.card.value.length"), String.valueOf(dto.getValueLength() != null ? dto.getValueLength() : 6)));
        add(createIconRow(VaadinIcon.ARROW_UP, I18n.t("kms.nextcode.card.increment"), String.valueOf(dto.getIncrement())));

        // Current numeric value row (with copy)
        HorizontalLayout currentRow = new HorizontalLayout();
        currentRow.setAlignItems(FlexComponent.Alignment.CENTER);
        currentRow.setSpacing(true);
        currentRow.setWidthFull();
        currentRow.addClassName("meta-row");

        Icon currentIcon = VaadinIcon.CALC.create();
        currentIcon.setSize("16px");
        currentIcon.addClassName("kms-partc-row-icon");

        Span currentLabel = new Span(I18n.t("kms.nextcode.card.current.value"));
        currentLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        currentLabel.addClassName(LumoUtility.FontSize.XSMALL);
        currentLabel.addClassName("kms-partc-row-label");

        codeValueSpan = new Span();
        codeValueSpan.addClassName(LumoUtility.FontWeight.BOLD);
        codeValueSpan.addClassName("kms-partc-row-value-mono");
        updateCodeValueDisplay();

        Button copyCurrentBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, String.valueOf(dto.getCodeValue()), I18n.t("kms.nextcode.card.copy.numeric"));
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
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("kms-partc-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("kms-partc-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("kms-partc-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void generateNextCode() {
        generateButton.setEnabled(false);
        try {
            String generated = generateCallback.apply(dto.getEntity(), dto.getAttribute());
            Notification.show(I18n.t("kms.nextcode.card.generated", generated), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            parentView.refreshCard(this);
        } catch (Exception e) {
            Notification.show(I18n.t("kms.nextcode.card.generate.error", e.getMessage()), 5000, Notification.Position.BOTTOM_END)
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