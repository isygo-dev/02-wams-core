package eu.isygoit.ui.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.ui.views.common.dialog.NoActionDialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class DecodeJwtDialog extends NoActionDialog {

    private final ObjectMapper objectMapper;
    private final String jwtToken;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));

    public DecodeJwtDialog(ObjectMapper objectMapper, String jwtToken) {
        super("Decoded JWT");
        this.objectMapper = objectMapper;
        this.jwtToken = jwtToken;

        setWidth("750px");
        setResizable(true);
        setDraggable(true);

        buildUI();
    }

    private void buildUI() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setMargin(false);

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                mainLayout.add(createErrorCard("Invalid JWT format – expected three parts (header, payload, signature)."));
                add(mainLayout);
                return;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode headerNode = objectMapper.readTree(headerJson);
            JsonNode payloadNode = objectMapper.readTree(payloadJson);

            String prettyHeader = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(headerNode);
            String prettyPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadNode);
            String displayPayload = transformPayloadWithInlineDates(prettyPayload, payloadNode);

            // Header card
            Card headerCard = createSectionCard("Header", prettyHeader, "JOSE Header (algorithm, type, etc.)");
            // Payload card
            Card payloadCard = createSectionCard("Payload", displayPayload, "Claims – the actual data (iat/exp show human dates)");

            mainLayout.add(headerCard, payloadCard);

            // Additional info (optional: signature preview)
            if (parts[2] != null && !parts[2].isEmpty()) {
                String signature = parts[2];
                Span sigInfo = new Span("Signature (base64url encoded): " + signature.substring(0, Math.min(20, signature.length())) + "...");
                sigInfo.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                sigInfo.getStyle().set("color", "var(--lumo-secondary-text-color)");
                mainLayout.add(sigInfo);
            }
        } catch (Exception e) {
            mainLayout.add(createErrorCard("Failed to decode JWT: " + e.getMessage()));
        }

        // Close button row
        Button closeBtn = new Button("Close", e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout buttonBar = new HorizontalLayout(closeBtn);
        buttonBar.setWidthFull();
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        mainLayout.add(buttonBar);

        add(mainLayout);
    }

    private Card createSectionCard(String title, String content, String tooltip) {
        Card card = new Card();
        card.setWidthFull();

        // Title row with copy button
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);

        Button copyButton = new Button(new Icon(VaadinIcon.COPY));
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        copyButton.setTooltipText("Copy to clipboard");
        copyButton.addClickListener(e -> copyToClipboard(content));

        HorizontalLayout titleRow = new HorizontalLayout(titleSpan, copyButton);
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");

        // Content area
        TextArea textArea = new TextArea();
        textArea.setValue(content);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.setHeight(title.equals("Header") ? "150px" : "300px");
        textArea.getStyle().set("font-family", "monospace");
        textArea.getStyle().set("font-size", "12px");
        textArea.getStyle().set("background", "var(--lumo-contrast-5pct)");
        textArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

        VerticalLayout cardContent = new VerticalLayout(titleRow, textArea);
        cardContent.setSpacing(false);
        cardContent.setPadding(false);
        card.add(cardContent);

        return card;
    }

    private Card createErrorCard(String message) {
        Card card = new Card();
        card.setWidthFull();
        Span errorSpan = new Span(VaadinIcon.EXCLAMATION_CIRCLE.create() + " " + message);
        errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        card.add(errorSpan);
        return card;
    }

    private void copyToClipboard(String text) {
        if (text == null || text.isBlank()) {
            Notification.show("Nothing to copy", 2000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => {" +
                        "  const notification = document.createElement('div');" +
                        "  notification.textContent = 'Copied to clipboard';" +
                        "  notification.style.position = 'fixed';" +
                        "  notification.style.bottom = '20px';" +
                        "  notification.style.left = '50%';" +
                        "  notification.style.transform = 'translateX(-50%)';" +
                        "  notification.style.backgroundColor = '#4caf50';" +
                        "  notification.style.color = 'white';" +
                        "  notification.style.padding = '8px 16px';" +
                        "  notification.style.borderRadius = '4px';" +
                        "  notification.style.zIndex = '10000';" +
                        "  document.body.appendChild(notification);" +
                        "  setTimeout(() => notification.remove(), 2000);" +
                        "});",
                text));
    }

    private String transformPayloadWithInlineDates(String prettyJson, JsonNode payloadNode) {
        String[] lines = prettyJson.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"iat\"") && payloadNode.has("iat") && payloadNode.get("iat").isNumber()) {
                long seconds = payloadNode.get("iat").asLong();
                String dateStr = DATE_FORMATTER.format(Instant.ofEpochSecond(seconds));
                line = line.replaceFirst("(:\\s*)(\\d+)(,?)", "$1$2 [" + dateStr + "]$3");
            } else if (trimmed.startsWith("\"exp\"") && payloadNode.has("exp") && payloadNode.get("exp").isNumber()) {
                long seconds = payloadNode.get("exp").asLong();
                String dateStr = DATE_FORMATTER.format(Instant.ofEpochSecond(seconds));
                line = line.replaceFirst("(:\\s*)(\\d+)(,?)", "$1$2 [" + dateStr + "]$3");
            }
            result.append(line).append("\n");
        }
        return result.toString();
    }
}