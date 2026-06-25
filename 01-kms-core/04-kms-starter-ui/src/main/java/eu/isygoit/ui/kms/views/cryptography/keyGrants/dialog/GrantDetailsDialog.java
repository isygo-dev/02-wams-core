package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

public class GrantDetailsDialog extends NoActionDialog {

    public GrantDetailsDialog(KmsDtos.ListGrantsResponse.Grant grant, ObjectMapper objectMapper) {
        super(I18n.t("grant.details.title"));
        setWidth("600px");
        setMaxWidth("95%");
        setResizable(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();

        content.add(createDetailRow(I18n.t("grant.details.field.grant.id"), grant.getGrantId()));
        content.add(createDetailRow(I18n.t("grant.details.field.grantee"), grant.getGranteePrincipal()));
        content.add(createDetailRow(I18n.t("grant.details.field.retiring"), grant.getRetiringPrincipal()));
        content.add(createDetailRow(I18n.t("grant.details.field.name"), grant.getName()));
        content.add(createDetailRow(I18n.t("grant.details.field.status"), grant.getStatus()));
        if (grant.getCreateDate() != null) {
            content.add(createDetailRow(I18n.t("grant.details.field.creation.date"), DateHelper.formatToHumanReadable(grant.getCreateDate())));
        }
        if (grant.getOperations() != null) {
            content.add(createDetailRow(I18n.t("grant.details.field.operations"), String.join(", ", grant.getOperations())));
        }
        if (grant.getConstraints() != null) {
            try {
                String constraintsJson = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(grant.getConstraints());
                Pre pre = new Pre(constraintsJson);
                pre.getStyle().set("background-color", "#f5f5f5").set("padding", "8px").set("border-radius", "4px");
                content.add(createDetailRow(I18n.t("grant.details.field.constraints"), pre));
            } catch (Exception e) {
                content.add(createDetailRow(I18n.t("grant.details.field.constraints"), grant.getConstraints().toString()));
            }
        }

        add(content);
    }

    private Span createDetailRow(String label, String value) {
        if (value == null) value = I18n.t("grant.details.placeholder");
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