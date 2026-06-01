package eu.isygoit.ui.views.tokenizer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.MainView;
import eu.isygoit.ui.views.BaseCard;
import eu.isygoit.ui.views.tokenizer.dialog.DeleteTokenConfigDialog;
import eu.isygoit.ui.views.tokenizer.dialog.UpdateTokenConfigDialog;

import java.util.List;

public class TokenConfigCard extends BaseCard<TokenConfigView, KmsTokenConfigService> {

    private TokenConfigDto dto;
    private final Runnable onDeleteRefresh;

    private Span titleSpan;
    private Span typeChip;

    public TokenConfigCard(TokenConfigView parentView,
                           KmsTokenConfigService tokenConfigService,
                           TokenConfigDto dto,
                           Runnable onDeleteRefresh) {
        super(parentView, tokenConfigService);
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
        addMetaRow(
                "Issuer: " + (dto.getIssuer() != null ? dto.getIssuer() : "—"),
                "Audience: " + (dto.getAudience() != null ? dto.getAudience() : "—")
        );
        addMetaRow(
                "Signature: " + (dto.getSignatureAlgorithm() != null ? dto.getSignatureAlgorithm() : "—")
        );

        // Secret key (masked) with copy button
        HorizontalLayout secretRow = new HorizontalLayout();
        secretRow.setAlignItems(FlexComponent.Alignment.CENTER);
        secretRow.setSpacing(true);
        Span secretLabel = new Span("Secret key:");
        secretLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        secretLabel.addClassName(LumoUtility.FontSize.SMALL);
        String secret = dto.getSecretKey() != null ? dto.getSecretKey() : "";
        String maskedSecret = secret.isEmpty() ? "—" : secret.substring(0, Math.min(4, secret.length())) + "..." + secret.substring(Math.max(0, secret.length() - 4));
        Span secretValue = new Span(maskedSecret);
        secretValue.getStyle().set("font-family", "monospace");
        secretValue.addClassName(LumoUtility.FontSize.XSMALL);
        secretValue.getElement().setAttribute("title", secret);
        Button copySecretBtn = MainView.createCopyButton(VaadinIcon.COPY, secret, "Copy full secret key");
        secretRow.add(secretLabel, secretValue, copySecretBtn);
        add(secretRow);
    }

    private void refreshDisplay() {
        titleSpan.setText(dto.getCode());
        titleSpan.getElement().setAttribute("title", dto.getCode());
        typeChip.setText(dto.getTokenType() != null ? dto.getTokenType().meaning() : "UNKNOWN");
        // For simplicity, we rebuild the body rows (or just update the necessary parts)
        // A more optimized approach would update individual components, but we'll refresh the whole card.
        getUI().ifPresent(ui -> ui.access(() -> {
            removeAll();
            buildHeader();
            buildBodyRows();
        }));
    }

    private void openEditDialog() {
        new UpdateTokenConfigDialog(objectService, dto, () -> parentView.refreshCard(this)).open();
    }
}