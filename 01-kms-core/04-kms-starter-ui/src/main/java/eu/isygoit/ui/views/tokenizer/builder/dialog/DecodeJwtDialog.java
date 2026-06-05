package eu.isygoit.ui.views.tokenizer.builder.dialog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import eu.isygoit.ui.views.common.dialog.NoActionDialog;

import java.util.Base64;

public class DecodeJwtDialog extends NoActionDialog {

    private final ObjectMapper objectMapper;
    private final String jwtToken;

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

            content.add(new Span("Header:"));
            TextArea headerArea = new TextArea();
            headerArea.setValue(prettyHeader);
            headerArea.setReadOnly(true);
            headerArea.setWidthFull();
            headerArea.setHeight("150px");
            headerArea.getStyle().set("font-family", "monospace");

            content.add(new Span("Payload:"));
            TextArea payloadArea = new TextArea();
            payloadArea.setValue(prettyPayload);
            payloadArea.setReadOnly(true);
            payloadArea.setWidthFull();
            payloadArea.setHeight("200px");
            payloadArea.getStyle().set("font-family", "monospace");

            content.add(headerArea, payloadArea);
        } catch (Exception e) {
            content.add(new Span("Failed to decode JWT: " + e.getMessage()));
        }

        Button closeBtn = new Button("Close", e -> close());
        content.add(closeBtn);
        add(content);
    }
}