package eu.isygoit.ui.views.keyGrants.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;

public class GrantDetailsDialog extends Dialog {

    public GrantDetailsDialog(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        setHeaderTitle("Grant Details");
        setWidth("600px");
        setResizable(true);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        content.add(createDetailRow("Grant ID", grant.getGrantId()));
        content.add(createDetailRow("Grantee Principal", grant.getGranteePrincipal()));
        content.add(createDetailRow("Retiring Principal", grant.getRetiringPrincipal()));
        content.add(createDetailRow("Name", grant.getName()));
        content.add(createDetailRow("Status", grant.getStatus()));
        if (grant.getCreateDate() != null) {
            content.add(createDetailRow("Creation Date", DateHelper.formatToHumanReadable(grant.getCreateDate())));
        }
        if (grant.getOperations() != null) {
            content.add(createDetailRow("Operations", String.join(", ", grant.getOperations())));
        }
        if (grant.getConstraints() != null) {
            try {
                String constraintsJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(grant.getConstraints());
                Pre pre = new Pre(constraintsJson);
                pre.getStyle().set("background-color", "#f5f5f5").set("padding", "8px").set("border-radius", "4px");
                content.add(createDetailRow("Constraints", pre));
            } catch (Exception e) {
                content.add(createDetailRow("Constraints", grant.getConstraints().toString()));
            }
        }

        add(content);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
    }

    private Span createDetailRow(String label, String value) {
        if (value == null) value = "-";
        Span span = new Span(label + ": " + value);
        span.getStyle().set("font-family", "monospace").set("font-size", "13px");
        return span;
    }

    private VerticalLayout createDetailRow(String label, Pre value) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle().set("font-weight", "bold").set("font-size", "13px");
        layout.add(labelSpan, value);
        return layout;
    }
}