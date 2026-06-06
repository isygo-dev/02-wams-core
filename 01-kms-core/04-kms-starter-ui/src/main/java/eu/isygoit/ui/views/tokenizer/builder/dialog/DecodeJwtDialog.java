package eu.isygoit.ui.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import eu.isygoit.ui.views.common.dialog.NoActionDialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecodeJwtDialog extends NoActionDialog {

    private final ObjectMapper objectMapper;
    private final String jwtToken;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    public DecodeJwtDialog(ObjectMapper objectMapper, String jwtToken) {
        super("Decoded JWT");
        this.objectMapper = objectMapper;
        this.jwtToken = jwtToken;

        setWidth("600px");
        setResizable(true);

        buildUI();
    }

    private void buildUI() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                content.add(new Span("Invalid JWT format – expected three parts."));
                add(content);
                return;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode headerNode = objectMapper.readTree(headerJson);
            JsonNode payloadNode = objectMapper.readTree(payloadJson);

            String prettyHeader = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(headerNode);
            String prettyPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadNode);

            // Transform payload to inline dates for iat and exp
            String displayPayload = transformPayloadWithInlineDates(prettyPayload, payloadNode);

            content.add(new Span("Header:"));
            TextArea headerArea = new TextArea();
            headerArea.setValue(prettyHeader);
            headerArea.setReadOnly(true);
            headerArea.setWidthFull();
            headerArea.setHeight("150px");
            headerArea.getStyle().set("font-family", "monospace");

            content.add(new Span("Payload:"));
            TextArea payloadArea = new TextArea();
            payloadArea.setValue(displayPayload);
            payloadArea.setReadOnly(true);
            payloadArea.setWidthFull();
            payloadArea.setHeight("250px");
            payloadArea.getStyle().set("font-family", "monospace");

            content.add(headerArea, payloadArea);
        } catch (Exception e) {
            content.add(new Span("Failed to decode JWT: " + e.getMessage()));
        }

        Button closeBtn = new Button("Close", e -> close());
        HorizontalLayout buttonBar = new HorizontalLayout(closeBtn);
        buttonBar.setWidthFull();
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        content.add(buttonBar);

        add(content);
    }

    private String transformPayloadWithInlineDates(String prettyJson, JsonNode payloadNode) {
        // Process line by line to preserve indentation
        String[] lines = prettyJson.split("\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("\"iat\"") && payloadNode.has("iat") && payloadNode.get("iat").isNumber()) {
                long seconds = payloadNode.get("iat").asLong();
                String dateStr = DATE_FORMATTER.format(Instant.ofEpochSecond(seconds));
                // Replace the numeric value with "value [date]"
                line = line.replaceFirst("(:\\s*)(\\d+)(,?)",
                        "$1$2 [" + dateStr + "]$3");
            } else if (trimmed.startsWith("\"exp\"") && payloadNode.has("exp") && payloadNode.get("exp").isNumber()) {
                long seconds = payloadNode.get("exp").asLong();
                String dateStr = DATE_FORMATTER.format(Instant.ofEpochSecond(seconds));
                line = line.replaceFirst("(:\\s*)(\\d+)(,?)",
                        "$1$2 [" + dateStr + "]$3");
            }
            result.append(line).append("\n");
        }
        return result.toString();
    }
}