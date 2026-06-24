package eu.isygoit.ui.kms.views.cryptography.random;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.RandomKeyDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.RandomKeyService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.KmsMainView;
import eu.isygoit.ui.kms.views.cryptography.random.dialog.DeleteRandomKeyDialog;
import eu.isygoit.ui.kms.views.cryptography.random.dialog.RenewRandomKeyDialog;

import java.util.List;

public class RandomKeyCard extends BaseCard<RandomKeyView, RandomKeyService> {

    private final RandomKeyDto dto;
    private Button renewButton;

    public RandomKeyCard(RandomKeyView parentView, RandomKeyService service, RandomKeyDto dto) {
        super(parentView, service);
        this.dto = dto;
        initCard();
    }

    public RandomKeyDto getDto() {
        return dto;
    }

    @Override
    protected String cardCssClassName() {
        return "random-key-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");
        Span titleSpan = buildTitleSpan(dto.getName(), null);
        left.add(titleSpan);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        renewButton = createIconButton(VaadinIcon.REFRESH, I18n.t("random.key.card.renew.tooltip"));
        renewButton.addClickListener(e -> new RenewRandomKeyDialog(objectService, dto.getName(),
                () -> parentView.refreshCard(this)).open());

        Button deleteButton = createDangerIconButton(VaadinIcon.TRASH, I18n.t("random.key.card.delete.tooltip"));
        deleteButton.addClickListener(e -> new DeleteRandomKeyDialog(objectService, dto.getName(),
                () -> parentView.removeCard(this)).open());

        return List.of(renewButton, deleteButton);
    }

    @Override
    protected void buildBodyRows() {
        // Creation date
        add(createIconRow(VaadinIcon.CALENDAR, I18n.t("random.key.card.created"),
                dto.getCreateDate() != null ? DateHelper.formatToHumanReadable(dto.getCreateDate()) : "—"));

        // Last update (renewal)
        add(createIconRow(VaadinIcon.REFRESH, I18n.t("random.key.card.last.renewed"),
                dto.getUpdateDate() != null ? DateHelper.formatToHumanReadable(dto.getUpdateDate()) : "—"));

        // Key value row with length indicator
        HorizontalLayout keyRow = new HorizontalLayout();
        keyRow.setAlignItems(FlexComponent.Alignment.CENTER);
        keyRow.setSpacing(true);
        keyRow.setWidthFull();
        keyRow.getStyle().set("margin-top", "var(--lumo-space-xs)");
        keyRow.addClassName("meta-row");

        Icon keyIcon = VaadinIcon.KEY.create();
        keyIcon.setSize("16px");
        keyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Compute key length and create label with parentheses
        int keyLength = dto.getValue() != null ? dto.getValue().length() : 0;
        String keyLabelText = I18n.t("random.key.card.key.value") + " " + I18n.t("random.key.card.key.value.length", keyLength);
        Span keyLabel = new Span(keyLabelText);
        keyLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        keyLabel.addClassName(LumoUtility.FontSize.XSMALL);
        keyLabel.getStyle().set("min-width", "100px");

        String maskedValue = maskKey(dto.getValue());
        Span keyValueSpan = new Span(maskedValue);
        keyValueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        keyValueSpan.getStyle().set("font-family", "monospace");
        keyValueSpan.getStyle().set("flex", "1");

        Button copyBtn = KmsMainView.createCopyButton(VaadinIcon.COPY, dto.getValue(), I18n.t("random.key.card.copy.tooltip"));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);

        keyRow.add(keyIcon, keyLabel, keyValueSpan, copyBtn);
        keyRow.expand(keyValueSpan);
        add(keyRow);
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

        Span valueSpan = new Span(value != null ? value : I18n.t("random.key.card.masked"));
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("font-family", "monospace");
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private String maskKey(String full) {
        if (full == null) return I18n.t("random.key.card.masked");
        if (full.length() <= 8) return "****";
        return full.substring(0, 4) + "..." + full.substring(full.length() - 4);
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .random-key-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .random-key-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .random-key-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .random-key-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}