package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.i18n.I18n;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class ViewTemplateDialog extends Dialog {

    private final MsgTemplateDto template;

    public ViewTemplateDialog(MsgTemplateDto template) {
        this.template = template;

        setHeaderTitle(I18n.t("template.dialog.view.title", template.getId()));
        setWidth("600px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        layout.add(buildContent());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        Button closeButton = new Button(I18n.t("template.dialog.close"));
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickListener(e -> close());

        buttons.add(closeButton);
        layout.add(buttons);

        add(layout);
    }

    private VerticalLayout buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(false);

        // Tenant
        addField(content, I18n.t("template.grid.tenant"), template.getTenant());

        // Name
        addField(content, I18n.t("template.grid.name"), template.getName());

        // Code
        addField(content, I18n.t("template.grid.code"), template.getCode());

        // Description
        addField(content, I18n.t("template.grid.description"), template.getDescription());

        // Language
        String lang = template.getLanguage() != null ? template.getLanguage().name() : "EN";
        addField(content, I18n.t("template.grid.language"), lang);

        // File name
        if (template.getFileName() != null) {
            HorizontalLayout fileRow = new HorizontalLayout();
            fileRow.setSpacing(true);
            fileRow.setAlignItems(FlexComponent.Alignment.CENTER);

            Paragraph fileLabel = new Paragraph(I18n.t("template.dialog.file") + ":");
            fileLabel.addClassName("font-weight-bold");

            Paragraph fileName = new Paragraph(template.getFileName());
            fileName.getStyle().set("font-family", "monospace");

            Button downloadBtn = new Button(VaadinIcon.DOWNLOAD.create());
            downloadBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            downloadBtn.setTooltipText(I18n.t("template.dialog.download"));
            downloadBtn.addClickListener(e -> {
                // In real implementation, download the file
                Notification.show(I18n.t("template.download.starting"), 3000, Notification.Position.BOTTOM_END);
            });

            fileRow.add(fileLabel, fileName, downloadBtn);
            content.add(fileRow);
        }

        return content;
    }

    private void addField(VerticalLayout layout, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setWidthFull();

        Paragraph labelParagraph = new Paragraph(label + ":");
        labelParagraph.addClassName("font-weight-bold");
        labelParagraph.getStyle().set("min-width", "120px");

        Paragraph valueParagraph = new Paragraph(value != null ? value : I18n.t("template.dialog.not.specified"));
        valueParagraph.getStyle().set("word-break", "break-all");

        row.add(labelParagraph, valueParagraph);
        row.expand(valueParagraph);
        layout.add(row);
    }
}