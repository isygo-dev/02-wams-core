package eu.isygoit.ui.kms.views.tokenizer.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.TokenRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsTokenService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.tokenizer.builder.dialog.ClaimsBuilderDialog;
import eu.isygoit.ui.kms.views.tokenizer.builder.dialog.DecodeJwtDialog;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@VaadinSessionScope
@Route(value = "kms/token-builder", layout = KmsMainLayout.class)
@PageTitle("Tokenizer – JWT Management")
@PermitAll
public class TokenBuilderView extends ManagementVerticalView {

    private final KmsTokenService tokenService;
    private final ObjectMapper objectMapper;

    private final ProgressBar globalLoadingBar = new ProgressBar();

    // Build section
    private final Card buildCard = new Card();
    private final ComboBox<IEnumToken.Types> tokenTypeCombo = new ComboBox<>();
    private final AudienceInput audienceInputBuilder = new AudienceInput();
    private final TextField subjectField = new TextField();
    private final TextArea claimsArea = new TextArea();
    private final Button buildClaimsButton = new Button(I18n.t("kms.token.builder.custom.claims.build"), new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button buildButton = new Button(I18n.t("kms.token.builder.generate.button"), new Icon(VaadinIcon.COG));
    private final VerticalLayout buildResultPanel = new VerticalLayout();

    // Validate section
    private final Card validateCard = new Card();
    private final ComboBox<IEnumToken.Types> validateTokenTypeCombo = new ComboBox<>();
    private final AudienceInput audienceInputValidator = new AudienceInput();
    private final TextArea validateTokenField = new TextArea();
    private final TextField validateSubjectField = new TextField();
    private final Button validateButton = new Button(I18n.t("kms.token.builder.validate.token"), new Icon(VaadinIcon.SEARCH));
    private final Span validationResultSpan = new Span();

    public TokenBuilderView(KmsTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("tokenizer-view");

        globalLoadingBar.setIndeterminate(true);
        globalLoadingBar.setVisible(false);
        globalLoadingBar.setWidthFull();
        add(globalLoadingBar);

        buildHeader();
        buildBuildCard();
        buildValidateCard();

        HorizontalLayout cardsRow = new HorizontalLayout(buildCard, validateCard);
        cardsRow.setWidthFull();
        cardsRow.setSpacing(true);
        cardsRow.addClassName("cards-row");
        cardsRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        add(cardsRow);
    }

    private void buildHeader() {
        H2 header = new H2(I18n.t("kms.token.builder.title"));
        header.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.NONE);
        Span subtitle = new Span(I18n.t("kms.token.builder.subtitle"));
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        add(header, subtitle);
    }

    private void buildBuildCard() {
        buildCard.setWidthFull();
        buildCard.addClassNames("compact-card");

        HorizontalLayout titleRow = new HorizontalLayout(new Icon(VaadinIcon.COG), new Span(I18n.t("kms.token.builder.generate.title")));
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.addClassName(LumoUtility.FontWeight.BOLD);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("100%");

        tokenTypeCombo.setLabel(I18n.t("kms.token.builder.token.type"));
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        tokenTypeCombo.setRequired(true);

        audienceInputBuilder.setLabelText(I18n.t("kms.token.builder.audiences"));

        subjectField.setLabel(I18n.t("kms.token.builder.subject"));
        subjectField.setPlaceholder(I18n.t("kms.token.builder.subject.placeholder"));
        subjectField.setRequired(true);

        HorizontalLayout claimsHeader = new HorizontalLayout();
        claimsHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        claimsHeader.setSpacing(true);
        Span claimsLabel = new Span(I18n.t("kms.token.builder.custom.claims"));
        claimsLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        buildClaimsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        buildClaimsButton.addClickListener(e -> openClaimsBuilderDialog());
        claimsHeader.add(claimsLabel, buildClaimsButton);
        claimsHeader.expand(claimsLabel);

        claimsArea.setHeight("200px");
        claimsArea.setPlaceholder(I18n.t("kms.token.builder.custom.claims.placeholder"));
        claimsArea.setHelperText(I18n.t("kms.token.builder.custom.claims.helper"));

        form.add(tokenTypeCombo, audienceInputBuilder, subjectField, claimsHeader, claimsArea);
        buildCard.add(titleRow, form);

        buildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        buildButton.setWidthFull();
        buildButton.addClickListener(e -> buildToken());
        buildCard.add(buildButton);

        buildResultPanel.setVisible(false);
        buildResultPanel.setPadding(false);
        buildResultPanel.setSpacing(false);
        buildResultPanel.addClassName("result-panel");
        buildCard.add(buildResultPanel);
    }

    private void buildValidateCard() {
        validateCard.setWidthFull();
        validateCard.addClassNames("compact-card");

        HorizontalLayout titleRow = new HorizontalLayout(new Icon(VaadinIcon.SEARCH), new Span(I18n.t("kms.token.builder.validate.title")));
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.addClassName(LumoUtility.FontWeight.BOLD);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("100%");

        validateTokenTypeCombo.setLabel(I18n.t("kms.token.builder.token.type"));
        validateTokenTypeCombo.setItems(IEnumToken.Types.values());
        validateTokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        validateTokenTypeCombo.setRequired(true);

        audienceInputValidator.setLabelText(I18n.t("kms.token.builder.validate.expected.audiences"));

        validateTokenField.setLabel(I18n.t("kms.token.builder.validate.token"));
        validateTokenField.setPlaceholder(I18n.t("kms.token.builder.validate.token.placeholder"));
        validateTokenField.setHeight("120px");
        validateTokenField.setRequired(true);

        validateSubjectField.setLabel(I18n.t("kms.token.builder.validate.subject"));
        validateSubjectField.setPlaceholder(I18n.t("kms.token.builder.validate.subject.placeholder"));
        validateSubjectField.setRequired(true);

        form.add(validateTokenTypeCombo, audienceInputValidator, validateSubjectField, validateTokenField);
        validateCard.add(titleRow, form);

        validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        validateButton.setWidthFull();
        validateButton.addClickListener(e -> validateToken());
        validateCard.add(validateButton);

        validationResultSpan.setVisible(false);
        validationResultSpan.addClassName(LumoUtility.FontWeight.BOLD);
        validateCard.add(validationResultSpan);
    }

    private void openClaimsBuilderDialog() {
        new ClaimsBuilderDialog(objectMapper, claimsArea.getValue(),
                prettyJson -> claimsArea.setValue(prettyJson)).open();
    }

    private void showDecodeDialog(String jwtToken) {
        new DecodeJwtDialog(objectMapper, jwtToken).open();
    }

    private void buildToken() {
        Set<String> audiences = audienceInputBuilder.getAudiences();
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        String subject = subjectField.getValue();
        String claimsJson = claimsArea.getValue();

        if (audiences.isEmpty()) {
            showError(I18n.t("kms.token.builder.audience.required"));
            return;
        }
        if (tokenType == null) {
            showError(I18n.t("kms.token.builder.token.type.required"));
            return;
        }
        if (!StringUtils.hasText(subject)) {
            showError(I18n.t("kms.token.builder.subject.required"));
            return;
        }

        Map<String, Object> claims = null;
        if (StringUtils.hasText(claimsJson)) {
            try {
                claims = objectMapper.readValue(claimsJson, Map.class);
            } catch (Exception e) {
                showError(I18n.t("kms.token.builder.claims.invalid", e.getMessage()));
                return;
            }
        }

        showGlobalLoading(true);
        buildButton.setEnabled(false);
        try {
            TokenRequestDto request = TokenRequestDto.builder().subject(subject).claims(claims).build();
            ResponseEntity<TokenResponseDto> response = tokenService.buildToken("super-tenant", audiences, tokenType, request);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                displayBuildResult(response.getBody());
                showSuccess(I18n.t("kms.token.builder.token.generated"));
            } else {
                showError(I18n.t("kms.token.builder.token.generation.failed", response.getStatusCode()));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError(I18n.t("kms.token.builder.build.error", errorMsg));
            buildResultPanel.setVisible(false);
        } catch (Exception e) {
            showError(I18n.t("kms.token.builder.unexpected.error", e.getMessage()));
            buildResultPanel.setVisible(false);
        } finally {
            showGlobalLoading(false);
            buildButton.setEnabled(true);
        }
    }

    private void displayBuildResult(TokenResponseDto tokenResponse) {
        buildResultPanel.removeAll();
        buildResultPanel.setVisible(true);

        HorizontalLayout tokenLine = new HorizontalLayout();
        tokenLine.setAlignItems(FlexComponent.Alignment.CENTER);
        tokenLine.setSpacing(true);
        tokenLine.setWidthFull();
        tokenLine.addClassName("token-result-line");

        Span tokenLabel = new Span("🔑 " + I18n.t("kms.token.builder.token.label"));
        tokenLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span tokenValue = new Span(tokenResponse.getToken());
        tokenValue.addClassName("token-result-value");

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyBtn.setTooltipText(I18n.t("kms.token.builder.copy.token"));
        copyBtn.addClickListener(e -> copyToClipboard(tokenResponse.getToken()));

        Button decryptBtn = new Button(new Icon(VaadinIcon.EYE));
        decryptBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        decryptBtn.setTooltipText(I18n.t("kms.token.builder.decode.jwt"));
        decryptBtn.addClickListener(e -> showDecodeDialog(tokenResponse.getToken()));

        tokenLine.add(tokenLabel, tokenValue, copyBtn, decryptBtn);
        tokenLine.expand(tokenValue);

        HorizontalLayout expiryLine = new HorizontalLayout();
        expiryLine.setAlignItems(FlexComponent.Alignment.CENTER);
        expiryLine.setSpacing(true);
        Span expiryLabel = new Span("📅 " + I18n.t("kms.token.builder.expires.label"));
        expiryLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        Span expiryValue = new Span(tokenResponse.getExpiryDate() != null ?
                DateHelper.formatToHumanReadable(DateHelper.toLocalDateTime(tokenResponse.getExpiryDate()))
                : I18n.t("kms.token.builder.expires.never"));
        expiryValue.addClassName("expiry-value");
        expiryLine.add(expiryLabel, expiryValue);

        buildResultPanel.add(tokenLine, expiryLine);
    }

    private void validateToken() {
        Set<String> audiences = audienceInputValidator.getAudiences();
        IEnumToken.Types tokenType = validateTokenTypeCombo.getValue();
        String token = validateTokenField.getValue();
        String subject = validateSubjectField.getValue();

        if (audiences.isEmpty()) {
            showError(I18n.t("kms.token.builder.audience.required.validation"));
            return;
        }
        if (tokenType == null) {
            showError(I18n.t("kms.token.builder.token.type.required"));
            return;
        }
        if (!StringUtils.hasText(token)) {
            showError(I18n.t("kms.token.builder.token.required"));
            return;
        }
        if (!StringUtils.hasText(subject)) {
            showError(I18n.t("kms.token.builder.subject.required"));
            return;
        }

        showGlobalLoading(true);
        validateButton.setEnabled(false);
        try {
            ResponseEntity<Boolean> response = tokenService.isTokenValid(audiences, tokenType, token, subject);
            validationResultSpan.setVisible(true);
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                validationResultSpan.setText("✅ " + I18n.t("kms.token.builder.token.valid"));
                validationResultSpan.removeClassName("validation-result-error");
                validationResultSpan.addClassName("validation-result-success");
                showSuccess(I18n.t("kms.token.builder.token.valid.success"));
            } else {
                validationResultSpan.setText("❌ " + I18n.t("kms.token.builder.token.invalid"));
                validationResultSpan.removeClassName("validation-result-success");
                validationResultSpan.addClassName("validation-result-error");
                showWarning(I18n.t("kms.token.builder.token.invalid.warning"));
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError(I18n.t("kms.token.builder.validate.error", errorMsg));
            validationResultSpan.setVisible(true);
            validationResultSpan.setText("❌ " + I18n.t("kms.token.builder.validation.failed", errorMsg));
            validationResultSpan.removeClassName("validation-result-success");
            validationResultSpan.addClassName("validation-result-error");
        } catch (Exception e) {
            showError(I18n.t("kms.token.builder.unexpected.error", e.getMessage()));
        } finally {
            showGlobalLoading(false);
            validateButton.setEnabled(true);
        }
    }

    private void showGlobalLoading(boolean show) {
        globalLoadingBar.setVisible(show);
        buildButton.setEnabled(!show);
        validateButton.setEnabled(!show);
    }

    private void copyToClipboard(String text) {
        if (!StringUtils.hasText(text)) return;
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => {" +
                        "  const notification = document.createElement('div');" +
                        "  notification.textContent = $1;" +
                        "  notification.style.position = 'fixed';" +
                        "  notification.style.bottom = '20px';" +
                        "  notification.style.right = '20px';" +
                        "  notification.style.backgroundColor = '#4caf50';" +
                        "  notification.style.color = 'white';" +
                        "  notification.style.padding = '8px 16px';" +
                        "  notification.style.borderRadius = '4px';" +
                        "  notification.style.zIndex = '1000';" +
                        "  document.body.appendChild(notification);" +
                        "  setTimeout(() => notification.remove(), 2000);" +
                        "}).catch(() => {" +
                        "  const notification = document.createElement('div');" +
                        "  notification.textContent = $2;" +
                        "  notification.style.backgroundColor = '#f44336';" +
                        "  notification.style.color = 'white';" +
                        "  notification.style.padding = '8px 16px';" +
                        "  notification.style.borderRadius = '4px';" +
                        "  notification.style.position = 'fixed';" +
                        "  notification.style.bottom = '20px';" +
                        "  notification.style.right = '20px';" +
                        "  notification.style.zIndex = '1000';" +
                        "  document.body.appendChild(notification);" +
                        "  setTimeout(() => notification.remove(), 2000);" +
                        "});",
                text, I18n.t("kms.token.builder.copy.success"), I18n.t("kms.token.builder.copy.failed"));
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null) return I18n.t("kms.token.builder.error.unknown");
        try {
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
            if (errorMap.containsKey("message")) return errorMap.get("message").toString();
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "…" : responseBody;
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String msg) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showWarning(String msg) {
        Notification.show(msg, 4000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    // AudienceInput component with i18n
    private static class AudienceInput extends VerticalLayout {
        private final TextField inputField;
        private final HorizontalLayout mainRow;
        private final HorizontalLayout chipsContainer;

        public AudienceInput() {
            setPadding(false);
            setSpacing(false);
            setWidthFull();

            Span labelSpan = new Span();
            labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            labelSpan.addClassName("audience-label");

            inputField = new TextField();
            inputField.setPlaceholder(I18n.t("kms.token.builder.audience.placeholder"));
            inputField.setWidthFull();

            Button addButton = new Button(new Icon(VaadinIcon.PLUS));
            addButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            addButton.addClickListener(e -> addAudience());

            mainRow = new HorizontalLayout(labelSpan, inputField, addButton);
            mainRow.setAlignItems(FlexComponent.Alignment.CENTER);
            mainRow.setSpacing(true);
            mainRow.setWidthFull();
            mainRow.expand(inputField);

            chipsContainer = new HorizontalLayout();
            chipsContainer.setSpacing(true);
            chipsContainer.setWidthFull();
            chipsContainer.addClassName("audience-chips-container");

            add(mainRow, chipsContainer);
        }

        public void setLabelText(String text) {
            Span labelSpan = (Span) mainRow.getComponentAt(0);
            labelSpan.setText(text);
        }

        private void addAudience() {
            String value = inputField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show(I18n.t("kms.token.builder.audience.empty"), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            value = value.trim();
            if (getAudiences().contains(value)) {
                Notification.show(I18n.t("kms.token.builder.audience.exists"), 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            addChip(value);
            inputField.clear();
        }

        private void addChip(String audience) {
            Span chip = new Span(audience);
            chip.addClassName("audience-chip");

            Button removeButton = new Button(new Icon(VaadinIcon.CLOSE_SMALL));
            removeButton.addThemeName("tertiary-inline");
            removeButton.addClassName("chip-remove");
            removeButton.addClickListener(e -> chipsContainer.remove(chip));
            chip.add(removeButton);
            chipsContainer.add(chip);
        }

        public Set<String> getAudiences() {
            Set<String> set = new LinkedHashSet<>();
            for (Component component : chipsContainer.getChildren().toList()) {
                if (component instanceof Span) {
                    String text = ((Span) component).getText();
                    text = text.replace("✕", "").trim();
                    if (!text.isEmpty()) set.add(text);
                }
            }
            return set;
        }

        public void clear() {
            chipsContainer.removeAll();
        }
    }
}