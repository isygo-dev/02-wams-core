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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.TokenRequestDto;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.remote.kms.KmsTokenService;
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
public class TokenBuilderView extends VerticalLayout implements BeforeEnterObserver {

    private final KmsTokenService tokenService;
    private final ObjectMapper objectMapper;

    private final ProgressBar globalLoadingBar = new ProgressBar();

    // Build section
    private final Card buildCard = new Card();
    private final ComboBox<IEnumToken.Types> tokenTypeCombo = new ComboBox<>();
    private final AudienceInput audienceInputBuilder = new AudienceInput();
    private final TextField subjectField = new TextField();
    private final TextArea claimsArea = new TextArea();
    private final Button buildClaimsButton = new Button("Build Claims", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button buildButton = new Button("Generate Token", new Icon(VaadinIcon.COG));
    private final VerticalLayout buildResultPanel = new VerticalLayout();

    // Validate section
    private final Card validateCard = new Card();
    private final ComboBox<IEnumToken.Types> validateTokenTypeCombo = new ComboBox<>();
    private final AudienceInput audienceInputValidator = new AudienceInput();
    private final TextArea validateTokenField = new TextArea();
    private final TextField validateSubjectField = new TextField();
    private final Button validateButton = new Button("Validate Token", new Icon(VaadinIcon.SEARCH));
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
        cardsRow.getStyle().set("flex-wrap", "wrap");
        cardsRow.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        add(cardsRow);

        attachResponsiveStyles();
    }

    private void buildHeader() {
        H2 header = new H2("JWT Tokenizer");
        header.addClassNames(LumoUtility.FontSize.XXLARGE, LumoUtility.Margin.Bottom.NONE, LumoUtility.Margin.Top.NONE);
        Span subtitle = new Span("Generate and validate signed JSON Web Tokens");
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        add(header, subtitle);
    }

    private void buildBuildCard() {
        buildCard.setWidthFull();
        buildCard.addClassNames("compact-card");

        HorizontalLayout titleRow = new HorizontalLayout(new Icon(VaadinIcon.COG), new Span("Generate Token"));
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.addClassName(LumoUtility.FontWeight.BOLD);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("100%");

        tokenTypeCombo.setLabel("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        tokenTypeCombo.setRequired(true);

        audienceInputBuilder.setLabelText("Audiences (at least one)");

        subjectField.setLabel("Subject");
        subjectField.setPlaceholder("user@domain.com or service‑name");
        subjectField.setRequired(true);

        HorizontalLayout claimsHeader = new HorizontalLayout();
        claimsHeader.setAlignItems(FlexComponent.Alignment.CENTER);
        claimsHeader.setSpacing(true);
        Span claimsLabel = new Span("Custom claims (JSON)");
        claimsLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        buildClaimsButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        buildClaimsButton.addClickListener(e -> openClaimsBuilderDialog());
        claimsHeader.add(claimsLabel, buildClaimsButton);
        claimsHeader.expand(claimsLabel);

        claimsArea.setHeight("200px");
        claimsArea.setPlaceholder("{\n  \"role\": \"admin\",\n  \"scope\": \"read write\"\n}");
        claimsArea.setHelperText("Optional – must be valid JSON. Use the builder to create claims easily.");

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

        HorizontalLayout titleRow = new HorizontalLayout(new Icon(VaadinIcon.SEARCH), new Span("Validate Token"));
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.addClassName(LumoUtility.FontWeight.BOLD);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.setMaxWidth("100%");

        validateTokenTypeCombo.setLabel("Token type");
        validateTokenTypeCombo.setItems(IEnumToken.Types.values());
        validateTokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        validateTokenTypeCombo.setRequired(true);

        audienceInputValidator.setLabelText("Expected audiences");

        validateTokenField.setLabel("JWT token");
        validateTokenField.setPlaceholder("Paste the full JWT string here");
        validateTokenField.setHeight("120px");
        validateTokenField.setRequired(true);

        validateSubjectField.setLabel("Expected subject");
        validateSubjectField.setPlaceholder("Subject that must match the token's 'sub' claim");
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
            showError("At least one audience is required");
            return;
        }
        if (tokenType == null) {
            showError("Token type is required");
            return;
        }
        if (!StringUtils.hasText(subject)) {
            showError("Subject is required");
            return;
        }

        Map<String, Object> claims = null;
        if (StringUtils.hasText(claimsJson)) {
            try {
                claims = objectMapper.readValue(claimsJson, Map.class);
            } catch (Exception e) {
                showError("Invalid claims JSON: " + e.getMessage());
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
                showSuccess("Token generated successfully");
            } else {
                showError("Token generation failed: " + response.getStatusCode());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError("Build error: " + errorMsg);
            buildResultPanel.setVisible(false);
        } catch (Exception e) {
            showError("Unexpected error: " + e.getMessage());
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
        tokenLine.getStyle().set("flex-wrap", "wrap");

        Span tokenLabel = new Span("🔑 Token:");
        tokenLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        Span tokenValue = new Span(tokenResponse.getToken());
        tokenValue.getStyle().set("font-family", "monospace");
        tokenValue.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        tokenValue.getStyle().set("word-break", "break-all");
        tokenValue.getStyle().set("flex", "1");

        Button copyBtn = new Button(new Icon(VaadinIcon.COPY));
        copyBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyBtn.setTooltipText("Copy token");
        copyBtn.addClickListener(e -> copyToClipboard(tokenResponse.getToken()));

        Button decryptBtn = new Button(new Icon(VaadinIcon.EYE));
        decryptBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        decryptBtn.setTooltipText("Decode JWT");
        decryptBtn.addClickListener(e -> showDecodeDialog(tokenResponse.getToken()));

        tokenLine.add(tokenLabel, tokenValue, copyBtn, decryptBtn);
        tokenLine.expand(tokenValue);

        HorizontalLayout expiryLine = new HorizontalLayout();
        expiryLine.setAlignItems(FlexComponent.Alignment.CENTER);
        expiryLine.setSpacing(true);
        Span expiryLabel = new Span("📅 Expires:");
        expiryLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        Span expiryValue = new Span(tokenResponse.getExpiryDate() != null ?
                DateHelper.formatToHumanReadable(DateHelper.toLocalDateTime(tokenResponse.getExpiryDate()))
                : "Never");
        expiryValue.getStyle().set("font-family", "monospace");
        expiryLine.add(expiryLabel, expiryValue);

        buildResultPanel.add(tokenLine, expiryLine);
    }

    private void validateToken() {
        Set<String> audiences = audienceInputValidator.getAudiences();
        IEnumToken.Types tokenType = validateTokenTypeCombo.getValue();
        String token = validateTokenField.getValue();
        String subject = validateSubjectField.getValue();

        if (audiences.isEmpty()) {
            showError("At least one audience is required for validation");
            return;
        }
        if (tokenType == null) {
            showError("Token type is required");
            return;
        }
        if (!StringUtils.hasText(token)) {
            showError("Token is required");
            return;
        }
        if (!StringUtils.hasText(subject)) {
            showError("Subject is required");
            return;
        }

        showGlobalLoading(true);
        validateButton.setEnabled(false);
        try {
            ResponseEntity<Boolean> response = tokenService.isTokenValid(audiences, tokenType, token, subject);
            validationResultSpan.setVisible(true);
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                validationResultSpan.setText("✅ Token is VALID");
                validationResultSpan.getStyle().set("color", "var(--lumo-success-text-color)");
                showSuccess("Token is valid");
            } else {
                validationResultSpan.setText("❌ Token is INVALID");
                validationResultSpan.getStyle().set("color", "var(--lumo-error-text-color)");
                showWarning("Token is invalid");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError("Validation error: " + errorMsg);
            validationResultSpan.setVisible(true);
            validationResultSpan.setText("❌ Validation failed: " + errorMsg);
            validationResultSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
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
                        "  notification.textContent = 'Copied to clipboard';" +
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
                        "  notification.textContent = 'Failed to copy';" +
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
                text);
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null) return "Unknown error";
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

    private void attachResponsiveStyles() {
        String css = """
                .tokenizer-view {
                    background: linear-gradient(145deg, var(--lumo-primary-color-10pct), var(--lumo-base-color) 70%);
                    min-height: 100vh;
                    animation: fadeIn 0.5s ease-out;
                }
                @keyframes fadeIn {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .tokenizer-view .compact-card {
                    flex: 1;
                    min-width: 280px;
                    padding: var(--lumo-space-m);
                    transition: all 0.2s ease;
                    background: var(--lumo-base-color);
                    border-radius: var(--lumo-border-radius-xl);
                    box-shadow: var(--lumo-box-shadow-m);
                }
                .tokenizer-view .compact-card .form-layout {
                    margin-top: var(--lumo-space-m);
                }
                .tokenizer-view .result-panel {
                    margin-top: var(--lumo-space-m);
                    padding: var(--lumo-space-s);
                    background: var(--lumo-contrast-5pct);
                    border-radius: var(--lumo-border-radius-m);
                }
                .tokenizer-view .chip-remove {
                    width: 16px;
                    height: 16px;
                    padding: 0;
                }
                @media (max-width: 768px) {
                    .tokenizer-view .compact-card {
                        min-width: 100%;
                        margin-bottom: var(--lumo-space-m);
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (VaadinSession.getCurrent().getAttribute("user") == null) {
            String currentPath = event.getLocation().getPath();
            event.forwardTo("login?redirect=" + currentPath);
        }
    }

    // AudienceInput component (unchanged)
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
            labelSpan.getStyle().set("margin-right", "var(--lumo-space-s)");

            inputField = new TextField();
            inputField.setPlaceholder("Enter audience (e.g., https://api.example.com)");
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
            chipsContainer.getStyle().set("flex-wrap", "wrap");

            add(mainRow, chipsContainer);
        }

        public void setLabelText(String text) {
            Span labelSpan = (Span) mainRow.getComponentAt(0);
            labelSpan.setText(text);
        }

        private void addAudience() {
            String value = inputField.getValue();
            if (value == null || value.isBlank()) {
                Notification.show("Audience cannot be empty", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            value = value.trim();
            if (getAudiences().contains(value)) {
                Notification.show("Audience already added", 2000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }
            addChip(value);
            inputField.clear();
        }

        private void addChip(String audience) {
            Span chip = new Span(audience);
            chip.getStyle()
                    .set("background-color", "var(--lumo-contrast-10pct)")
                    .set("border-radius", "16px")
                    .set("padding", "4px 12px")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("display", "inline-flex")
                    .set("align-items", "center")
                    .set("gap", "8px");

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