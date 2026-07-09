package eu.isygoit.ui.kms.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Read-only dialog showing the decoded Header/Payload/Signature of a JWT.
 * Structurally different from the field-grid "…DetailsViewDialog"s: content
 * is multi-line JSON rendered in read-only {@link TextArea}s inside titled
 * section-cards, rather than label/value fields.
 */
public class JwtDetailsViewDialog extends DetailsViewDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));
    private final ObjectMapper objectMapper;
    private final String jwtToken;

    public JwtDetailsViewDialog(ObjectMapper objectMapper, String jwtToken) {
        super(I18n.t("kms.decode.jwt.title"));
        this.objectMapper = objectMapper;
        this.jwtToken = jwtToken;

        addClassName("decode-jwt-dialog");
        setWidth("750px");
        setMaxWidth("95%");
        setResizable(true);
        setDraggable(true);

        buildUI();
    }

    private void buildUI() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setWidthFull();

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                mainLayout.add(createErrorCard(I18n.t("kms.decode.jwt.invalid.format")));
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
            Card headerCard = createSectionCard(
                    I18n.t("kms.decode.jwt.header"),
                    prettyHeader,
                    I18n.t("kms.decode.jwt.header.tooltip")
            );
            // Payload card
            Card payloadCard = createSectionCard(
                    I18n.t("kms.decode.jwt.payload"),
                    displayPayload,
                    I18n.t("kms.decode.jwt.payload.tooltip")
            );

            mainLayout.add(headerCard, payloadCard);

            // Signature info
            if (parts[2] != null && !parts[2].isEmpty()) {
                String signature = parts[2];
                mainLayout.add(createSignatureRow(signature));
            }
        } catch (Exception e) {
            mainLayout.add(createErrorCard(I18n.t("kms.decode.jwt.decode.failed", e.getMessage())));
        }

        // No ad-hoc button row here: NoActionDialog already supplies the standard
        // footer (error slot + Close button), so every dialog in the app shares
        // the same footer shape.
        add(mainLayout);
    }

    private Card createSectionCard(String title, String content, String tooltip) {
        Card card = new Card();
        card.setWidthFull();

        // Title row with shared copy-to-clipboard button
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        titleSpan.setTitle(tooltip);

        HorizontalLayout titleRow = new HorizontalLayout(titleSpan, new ClipboardCopyButton(content));
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        titleRow.setSpacing(true);
        titleRow.addClassName("section-title-row");

        // Content area
        boolean isHeaderSection = title.equals(I18n.t("kms.decode.jwt.header"));
        TextArea textArea = new TextArea();
        textArea.setValue(content);
        textArea.setReadOnly(true);
        textArea.setWidthFull();
        textArea.addClassName("code-textarea");
        textArea.addClassName(isHeaderSection ? "code-textarea--header" : "code-textarea--payload");

        VerticalLayout cardContent = new VerticalLayout(titleRow, textArea);
        cardContent.setSpacing(false);
        cardContent.setPadding(false);
        card.add(cardContent);

        return card;
    }

    private HorizontalLayout createSignatureRow(String signature) {
        Span sigInfo = new Span(I18n.t("kms.decode.jwt.signature", signature.substring(0, Math.min(20, signature.length()))));
        sigInfo.addClassName("signature-info");

        HorizontalLayout row = new HorizontalLayout(sigInfo, new ClipboardCopyButton(signature));
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        return row;
    }

    private Card createErrorCard(String message) {
        Card card = new Card();
        card.setWidthFull();
        Span errorSpan = new Span(VaadinIcon.EXCLAMATION_CIRCLE.create() + " " + message);
        errorSpan.addClassName("error-message");
        card.add(errorSpan);
        return card;
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
