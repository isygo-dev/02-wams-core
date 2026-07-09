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
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.kms.views.cryptography.random.RandomKeyView;

/**
 * Read-only "Details" dialog for a {@link RandomKeyDto}.
 *
 * <p>Unlike other *DetailsViewDialog implementations in this codebase, {@code RandomKeyServiceApi}
 * exposes no {@code findById}/{@code findByName} lookup for a single random key (only listing,
 * creation, renewal and deletion by name). The dialog therefore renders the {@link RandomKeyDto}
 * already held by the card/view instead of re-fetching it from the service.</p>
 */
public class RandomKeyDetailsViewDialog extends DetailsViewDialog {

    private final RandomKeyView parentView;
    private final RandomKeyService keyService;
    private final RandomKeyDto dto;

    private boolean valueRevealed = false;
    private Span valueSpan;
    private Button revealButton;

    public RandomKeyDetailsViewDialog(RandomKeyView parentView, RandomKeyService keyService, RandomKeyDto dto) {
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

        // Section 1: Identity — name/tenant only.
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.random.key.dialog.details.field.name"), dto.getName(), true);
        addFieldToGrid(identityGrid, VaadinIcon.BUILDING, I18n.t("kms.random.key.dialog.details.field.tenant"), dto.getTenant());
        mainLayout.add(createSection(I18n.t("kms.random.key.dialog.details.section.identity"), identityGrid));

        // Section 2: Key value — masked value + reveal/copy.
        mainLayout.add(createSection(I18n.t("kms.random.key.dialog.details.section.value"), buildValueRow()));

        // Section 3: Audit — created/updated by/date.
        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("kms.random.key.dialog.details.field.created.by"), dto.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.random.key.dialog.details.field.created.date"),
                dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("kms.random.key.dialog.details.field.updated.by"), dto.getUpdatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.random.key.dialog.details.field.updated.date"),
                dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : null);
        mainLayout.add(createSection(I18n.t("kms.random.key.dialog.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    /**
     * Same masked/reveal/copy UX as {@code RandomKeyCard}, just reused here in a full-width row.
     */
    private Component buildValueRow() {
        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon keyIcon = VaadinIcon.KEY.create();
        keyIcon.setSize("12px");
        keyIcon.addClassName("detail-field-icon");

        int keyLength = dto.getValue() != null ? dto.getValue().length() : 0;
        String labelText = I18n.t("kms.random.key.dialog.details.field.value") + " "
                + I18n.t("kms.random.key.card.key.value.length", keyLength);
        Span labelSpan = new Span(labelText);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(keyIcon, labelSpan);

        HorizontalLayout valueRow = new HorizontalLayout();
        valueRow.setAlignItems(FlexComponent.Alignment.CENTER);
        valueRow.setSpacing(true);
        valueRow.setWidthFull();

        valueSpan = new Span(maskKey(dto.getValue()));
        valueSpan.addClassName("wams-card__detail-field-value");

        revealButton = new Button(new Icon(VaadinIcon.EYE));
        revealButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        revealButton.setTooltipText(I18n.t("kms.random.key.dialog.details.reveal.tooltip"));
        revealButton.addClickListener(e -> toggleReveal());

        Button copyBtn = new ClipboardCopyButton(dto.getValue(), I18n.t("kms.random.key.card.copy.tooltip"));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        valueRow.add(valueSpan, revealButton, copyBtn);
        valueRow.expand(valueSpan);

        field.add(labelRow, valueRow);
        return field;
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
}
