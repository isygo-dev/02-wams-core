package eu.isygoit.ui.kms.views.cryptography.random.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.random.RandomKeyView;

/**
 * Read-only "Details" dialog for a {@link RandomKeyDto}.
 *
 * <p>Unlike other *DetailsDialog implementations in this codebase, {@code RandomKeyServiceApi}
 * exposes no {@code findById}/{@code findByName} lookup for a single random key (only listing,
 * creation, renewal and deletion by name). The dialog therefore renders the {@link RandomKeyDto}
 * already held by the card/view instead of re-fetching it from the service.</p>
 */
public class RandomKeyDetailsDialog extends NoActionDialog {

    private final RandomKeyView parentView;
    private final RandomKeyService keyService;
    private final RandomKeyDto dto;

    private boolean valueRevealed = false;
    private Span valueSpan;
    private Button revealButton;

    public RandomKeyDetailsDialog(RandomKeyView parentView, RandomKeyService keyService, RandomKeyDto dto) {
        super(I18n.t("kms.random.key.dialog.details.title"));
        this.parentView = parentView;
        this.keyService = keyService;
        this.dto = dto;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("random-key-details-dialog");

        buildContent();
    }

    private void buildContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div infoGrid = new Div();
        infoGrid.addClassName("wams-card__detail-grid");

        addFieldToGrid(infoGrid, VaadinIcon.TAG, I18n.t("kms.random.key.dialog.details.field.name"), dto.getName());
        addFieldToGrid(infoGrid, VaadinIcon.BUILDING, I18n.t("kms.random.key.dialog.details.field.tenant"), dto.getTenant());
        addFieldToGrid(infoGrid, VaadinIcon.USER_CHECK, I18n.t("kms.random.key.dialog.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, I18n.t("kms.random.key.dialog.details.field.created.date"),
                dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.EDIT, I18n.t("kms.random.key.dialog.details.field.updated.by"), dto.getUpdatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.random.key.dialog.details.field.updated.date"),
                dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);

        mainLayout.add(createSection(I18n.t("kms.random.key.dialog.details.section.info"), infoGrid));
        mainLayout.add(createSection(I18n.t("kms.random.key.dialog.details.section.value"), buildValueRow()));

        add(mainLayout);
        addCloseButton();
    }

    /**
     * Same masked/reveal/copy UX as {@code RandomKeyCard}, just reused here in a full-width row.
     */
    private Component buildValueRow() {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon keyIcon = VaadinIcon.KEY.create();
        keyIcon.setSize("16px");
        keyIcon.addClassName("detail-field-icon");

        int keyLength = dto.getValue() != null ? dto.getValue().length() : 0;
        String labelText = I18n.t("kms.random.key.dialog.details.field.value") + " "
                + I18n.t("kms.random.key.card.key.value.length", keyLength);
        Span labelSpan = new Span(labelText);
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        valueSpan = new Span(maskKey(dto.getValue()));
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-field-value");

        revealButton = new Button(new Icon(VaadinIcon.EYE));
        revealButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        revealButton.setTooltipText(I18n.t("kms.random.key.dialog.details.reveal.tooltip"));
        revealButton.addClickListener(e -> toggleReveal());

        Button copyBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, dto.getValue(), I18n.t("kms.random.key.card.copy.tooltip"));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        row.add(keyIcon, labelSpan, valueSpan, revealButton, copyBtn);
        row.expand(valueSpan);
        return row;
    }

    private void toggleReveal() {
        valueRevealed = !valueRevealed;
        valueSpan.setText(valueRevealed ? (dto.getValue() != null ? dto.getValue() : I18n.t("kms.random.key.card.masked"))
                : maskKey(dto.getValue()));
        revealButton.setIcon(new Icon(valueRevealed ? VaadinIcon.EYE_SLASH : VaadinIcon.EYE));
        revealButton.setTooltipText(valueRevealed
                ? I18n.t("kms.random.key.dialog.details.hide.tooltip")
                : I18n.t("kms.random.key.dialog.details.reveal.tooltip"));
    }

    private String maskKey(String full) {
        if (full == null) return I18n.t("kms.random.key.card.masked");
        if (full.length() <= 8) return "****";
        return full.substring(0, 4) + "..." + full.substring(full.length() - 4);
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-field-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button(I18n.t("kms.random.key.dialog.details.close"), e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
    }
}
