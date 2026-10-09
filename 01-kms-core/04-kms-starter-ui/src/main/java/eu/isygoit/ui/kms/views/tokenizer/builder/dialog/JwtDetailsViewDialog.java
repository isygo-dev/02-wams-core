package eu.isygoit.ui.kms.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Read-only dialog showing the decoded Header/Payload/Signature of a JWT.
 * There is no DTO: the content is the decoded token. Structurally different
 * from the field-grid "...DetailsViewDialog"s: content is multi-line JSON
 * rendered in read-only {@link TextArea}s, one tab per JWT part.
 */
public class JwtDetailsViewDialog extends KmsDetailsDialog {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneId.of("UTC"));
    private static final String HEADER_MIN_HEIGHT = "10rem";
    private static final String PAYLOAD_MIN_HEIGHT = "20rem";

    private final ObjectMapper objectMapper;
    private final String jwtToken;

    public JwtDetailsViewDialog(ObjectMapper objectMapper, String jwtToken) {
        super(I18n.t("kms.decode.jwt.title"));
        this.objectMapper = objectMapper;
        this.jwtToken = jwtToken;

        applyWidth(DialogLayout.WIDTH_L);
        setDraggable(true);

        buildUI();
    }

    private void buildUI() {
        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                add(createErrorMessage(I18n.t("kms.decode.jwt.invalid.format")));
                return;
            }

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));

            JsonNode headerNode = objectMapper.readTree(headerJson);
            JsonNode payloadNode = objectMapper.readTree(payloadJson);

            String prettyHeader = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(headerNode);
            String prettyPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payloadNode);
            String displayPayload = transformPayloadWithInlineDates(prettyPayload, payloadNode);

            addTab(I18n.t("kms.decode.jwt.header"), buildCodeSection(
                    I18n.t("kms.decode.jwt.header"),
                    prettyHeader,
                    I18n.t("kms.decode.jwt.header.tooltip"),
                    HEADER_MIN_HEIGHT));
            addTab(I18n.t("kms.decode.jwt.payload"), buildCodeSection(
                    I18n.t("kms.decode.jwt.payload"),
                    displayPayload,
                    I18n.t("kms.decode.jwt.payload.tooltip"),
                    PAYLOAD_MIN_HEIGHT));

            // Signature info
            if (parts[2] != null && !parts[2].isEmpty()) {
                addTab(I18n.t("kms.decode.jwt.signature"), createSignatureRow(parts[2]));
            }
        } catch (Exception e) {
            add(createErrorMessage(I18n.t("kms.decode.jwt.decode.failed", e.getMessage())));
        }
    }

    private VerticalLayout buildCodeSection(String title, String content, String tooltip, String minHeight) {
        VerticalLayout section = DialogLayout.section(title, VaadinIcon.CODE);

        TextArea textArea = DialogLayout.tall(new TextArea());
        textArea.setValue(content);
        textArea.setReadOnly(true);
        textArea.setMinHeight(minHeight);
        textArea.setAriaLabel(title);
        textArea.setTooltipText(tooltip);
        textArea.addClassName(LumoUtility.FontSize.XSMALL);

        // Shared copy-to-clipboard button
        HorizontalLayout actions = new HorizontalLayout(new ClipboardCopyButton(content));
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        section.add(textArea, actions);
        return section;
    }

    private HorizontalLayout createSignatureRow(String signature) {
        Span sigInfo = new Span(I18n.t("kms.decode.jwt.signature", signature.substring(0, Math.min(20, signature.length()))));
        sigInfo.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);

        HorizontalLayout row = new HorizontalLayout(sigInfo, new ClipboardCopyButton(signature));
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        return row;
    }

    private Span createErrorMessage(String message) {
        Span error = new Span(VaadinIcon.EXCLAMATION_CIRCLE.create(), new Span(" " + message));
        error.addClassName(LumoUtility.TextColor.ERROR);
        return error;
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
