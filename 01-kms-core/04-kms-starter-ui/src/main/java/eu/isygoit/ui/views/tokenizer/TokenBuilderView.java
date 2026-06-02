package eu.isygoit.ui.views.tokenizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.TokenResponseDto;
import eu.isygoit.dto.data.TokenRequestDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenService;
import eu.isygoit.ui.MainLayout;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Route(value = "tokenizer", layout = MainLayout.class)
@PageTitle("Tokenizer - JWT Management")
@PermitAll
public class TokenBuilderView extends VerticalLayout {

    private final KmsTokenService tokenService;
    private final ObjectMapper objectMapper;

    // Build token components
    private final ComboBox<IEnumToken.Types> tokenTypeCombo = new ComboBox<>("Token type");
    private final TextField buildAudienceField = new TextField("Audience");
    private final TextField subjectField = new TextField("Subject");
    private final TextArea claimsArea = new TextArea("Claims (JSON)");
    private final Button buildButton = new Button("Build Token", new Icon(VaadinIcon.COG));
    private final ProgressBar loadingBar = new ProgressBar();

    // Result display
    private final Card resultCard = new Card();
    private final Span tokenResultSpan = new Span();
    private final Span expiryResultSpan = new Span();

    // Validate token components
    private final ComboBox<IEnumToken.Types> validateTokenTypeCombo = new ComboBox<>("Token type");
    private final TextField validateAudienceField = new TextField("Audience");
    private final TextField validateTokenField = new TextField("Token");
    private final TextField validateSubjectField = new TextField("Subject");
    private final Button validateButton = new Button("Validate Token", new Icon(VaadinIcon.SEARCH));
    private final Span validationResultSpan = new Span();

    public TokenBuilderView(KmsTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("tokenizer-view");

        buildHeader();
        buildBuildTokenSection();
        buildValidateTokenSection();
        attachResponsiveStyles();
    }

    private void buildHeader() {
        H2 header = new H2("Tokenizer");
        header.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.Margin.Bottom.NONE,
                LumoUtility.Margin.Top.NONE
        );
        Span subtitle = new Span("Manage JSON Web Tokens (JWT) – build and validate tokens for your audiences");
        subtitle.addClassName(LumoUtility.TextColor.SECONDARY);
        subtitle.addClassName(LumoUtility.FontSize.SMALL);
        add(header, subtitle);
    }

    private void buildBuildTokenSection() {
        Card card = new Card();
        card.addClassName("build-card");
        card.setWidthFull();

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        Icon buildIcon = VaadinIcon.COG.create();
        buildIcon.getStyle().set("color", "var(--lumo-primary-color)");
        H3 title = new H3("Generate new token");
        title.addClassName(LumoUtility.Margin.NONE);
        headerLayout.add(buildIcon, title);
        card.add(headerLayout);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setWidthFull();

        buildAudienceField.setRequired(true);
        buildAudienceField.setPlaceholder("e.g., kms-console");
        buildAudienceField.setTooltipText("Audience name (required)");
        buildAudienceField.setWidthFull();

        subjectField.setRequired(true);
        subjectField.setPlaceholder("e.g., user@example.com or service-account");
        subjectField.setTooltipText("Token subject (required)");
        subjectField.setWidthFull();

        claimsArea.setHeight("120px");
        claimsArea.setPlaceholder("Optional JSON claims:\n{\n  \"role\": \"admin\",\n  \"scope\": \"read write\"\n}");
        claimsArea.setHelperText("Valid JSON object with extra claims");

        form.add(tokenTypeCombo, buildAudienceField, subjectField, claimsArea);
        form.setColspan(claimsArea, 2);
        card.add(form);

        buildButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buildButton.addClickListener(e -> buildToken());
        card.add(buildButton);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        card.add(loadingBar);

        // Result card (initially hidden)
        resultCard.setVisible(false);
        resultCard.addClassName("result-card");
        resultCard.getStyle().set("margin-top", "var(--lumo-space-m)");
        resultCard.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        resultCard.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        // Token line with copy button on the right
        HorizontalLayout tokenLine = new HorizontalLayout();
        tokenLine.setAlignItems(FlexComponent.Alignment.CENTER);
        tokenLine.setSpacing(true);
        tokenLine.setWidthFull();
        tokenLine.getStyle().set("flex-wrap", "wrap");

        Span tokenLabel = new Span("🔑 Token:");
        tokenLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);

        tokenResultSpan.getStyle().set("font-family", "monospace");
        tokenResultSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        tokenResultSpan.getStyle().set("word-break", "break-all");
        tokenResultSpan.getStyle().set("flex", "1");

        Button copyTokenButton = new Button(new Icon(VaadinIcon.COPY));
        copyTokenButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyTokenButton.setTooltipText("Copy token");
        copyTokenButton.addClickListener(e -> copyTokenToClipboard());

        tokenLine.add(tokenLabel, tokenResultSpan, copyTokenButton);
        tokenLine.expand(tokenResultSpan);

        // Expiry line (no copy)
        HorizontalLayout expiryLine = new HorizontalLayout();
        expiryLine.setAlignItems(FlexComponent.Alignment.CENTER);
        expiryLine.setSpacing(true);
        Span expiryLabel = new Span("📅 Expires:");
        expiryLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        expiryResultSpan.getStyle().set("font-family", "monospace");
        expiryResultSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        expiryLine.add(expiryLabel, expiryResultSpan);

        resultCard.add(tokenLine, expiryLine);
        card.add(resultCard);

        add(card);
    }

    private void buildValidateTokenSection() {
        Card card = new Card();
        card.addClassName("validate-card");
        card.setWidthFull();
        card.getStyle().set("margin-top", "var(--lumo-space-l)");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setSpacing(true);
        Icon validateIcon = VaadinIcon.SEARCH.create();
        validateIcon.getStyle().set("color", "var(--lumo-primary-color)");
        H3 title = new H3("Validate token");
        title.addClassName(LumoUtility.Margin.NONE);
        headerLayout.add(validateIcon, title);
        card.add(headerLayout);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        validateTokenTypeCombo.setItems(IEnumToken.Types.values());
        validateTokenTypeCombo.setValue(IEnumToken.Types.ACCESS);
        validateTokenTypeCombo.setRequired(true);

        validateAudienceField.setRequired(true);
        validateAudienceField.setPlaceholder("e.g., kms-console");

        validateTokenField.setRequired(true);
        validateTokenField.setPlaceholder("Paste the JWT token here");
        validateTokenField.setWidthFull();

        validateSubjectField.setRequired(true);
        validateSubjectField.setPlaceholder("Subject expected in token");

        form.add(validateTokenTypeCombo, validateAudienceField, validateTokenField, validateSubjectField);
        form.setColspan(validateTokenField, 2);
        card.add(form);

        validateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        validateButton.addClickListener(e -> validateToken());
        card.add(validateButton);

        validationResultSpan.setVisible(false);
        validationResultSpan.addClassName(LumoUtility.FontWeight.BOLD);
        card.add(validationResultSpan);

        add(card);
    }

    private void buildToken() {
        String audience = buildAudienceField.getValue();
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        String subject = subjectField.getValue();
        String claimsJson = claimsArea.getValue();

        if (!StringUtils.hasText(audience)) {
            showError("Audience is required");
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

        showLoading(true);
        try {
            TokenRequestDto request = TokenRequestDto.builder()
                    .subject(subject)
                    .claims(claims)
                    .build();

            ResponseEntity<TokenResponseDto> response = tokenService.buildToken(audience, tokenType, request);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                TokenResponseDto tokenResponse = response.getBody();
                tokenResultSpan.setText(tokenResponse.getToken());
                expiryResultSpan.setText(formatDate(tokenResponse.getExpiryDate()));
                resultCard.setVisible(true);
                showSuccess("Token generated successfully");
            } else {
                showError("Failed to generate token: " + response.getStatusCode());
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError("Build error: " + errorMsg);
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private void copyTokenToClipboard() {
        String token = tokenResultSpan.getText();
        if (!StringUtils.hasText(token)) {
            showWarning("No token to copy");
            return;
        }
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = 'Token copied to clipboard'; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#4caf50'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 2000); " +
                        "}).catch(() => { " +
                        "  const notification = document.createElement('div'); " +
                        "  notification.textContent = 'Failed to copy token'; " +
                        "  notification.style.position = 'fixed'; " +
                        "  notification.style.bottom = '20px'; " +
                        "  notification.style.right = '20px'; " +
                        "  notification.style.backgroundColor = '#f44336'; " +
                        "  notification.style.color = 'white'; " +
                        "  notification.style.padding = '10px 20px'; " +
                        "  notification.style.borderRadius = '4px'; " +
                        "  notification.style.zIndex = '1000'; " +
                        "  document.body.appendChild(notification); " +
                        "  setTimeout(() => notification.remove(), 3000); " +
                        "});",
                token
        );
    }

    private void validateToken() {
        String audience = validateAudienceField.getValue();
        IEnumToken.Types tokenType = validateTokenTypeCombo.getValue();
        String token = validateTokenField.getValue();
        String subject = validateSubjectField.getValue();

        if (!StringUtils.hasText(audience)) {
            showError("Audience is required for validation");
            return;
        }
        if (tokenType == null) {
            showError("Token type is required for validation");
            return;
        }
        if (!StringUtils.hasText(token)) {
            showError("Token is required");
            return;
        }
        if (!StringUtils.hasText(subject)) {
            showError("Subject is required for validation");
            return;
        }

        showLoading(true);
        try {
            ResponseEntity<Boolean> response = tokenService.isTokenValid(audience, tokenType, token, subject);
            validationResultSpan.setVisible(true);
            if (response.getStatusCode().is2xxSuccessful() && Boolean.TRUE.equals(response.getBody())) {
                validationResultSpan.setText(" ✅ Token is VALID");
                validationResultSpan.getStyle().set("color", "var(--lumo-success-text-color)");
                showSuccess("Token is valid");
            } else {
                validationResultSpan.setText(" ❌ Token is INVALID");
                validationResultSpan.getStyle().set("color", "var(--lumo-error-text-color)");
                showWarning("Token is invalid");
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? extractErrorMessage(ex.contentUTF8()) : ex.getMessage();
            showError("Validation error: " + errorMsg);
            validationResultSpan.setVisible(true);
            validationResultSpan.setText(" ❌ Validation failed: " + errorMsg);
            validationResultSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null) return "Unknown error";
        try {
            Map<String, Object> errorMap = objectMapper.readValue(responseBody, Map.class);
            if (errorMap.containsKey("message")) {
                return errorMap.get("message").toString();
            }
        } catch (Exception ignored) {
        }
        return responseBody.length() > 200 ? responseBody.substring(0, 200) + "…" : responseBody;
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        buildButton.setEnabled(!show);
        validateButton.setEnabled(!show);
    }

    private void showSuccess(String msg) {
        Notification.show(msg, 4000, Notification.Position.BOTTOM_END)
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

    private String formatDate(Date date) {
        if (date == null) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        return sdf.format(date);
    }

    private void attachResponsiveStyles() {
        String css = """
                .tokenizer-view .build-card,
                .tokenizer-view .validate-card {
                    transition: all 0.2s ease;
                }
                .tokenizer-view .result-card {
                    transition: all 0.2s ease;
                }
                @media (max-width: 768px) {
                    .tokenizer-view .vaadin-form-layout {
                        flex-direction: column;
                    }
                    .tokenizer-view .result-card .token-line {
                        flex-direction: column;
                        align-items: flex-start;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }
}