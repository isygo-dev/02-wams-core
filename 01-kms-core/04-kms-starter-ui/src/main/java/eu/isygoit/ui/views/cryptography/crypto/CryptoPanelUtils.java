package eu.isygoit.ui.views.cryptography.crypto;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.springframework.util.StringUtils;

public final class CryptoPanelUtils {

    private CryptoPanelUtils() {
    }

    public static VerticalLayout createLabelledTextArea(String labelText, TextArea textArea) {
        VerticalLayout container = new VerticalLayout();
        container.setPadding(false);
        container.setSpacing(false);
        container.setWidthFull();
        container.addClassName("labelled-textarea-container");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        header.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

        // Replace deprecated Label with Span
        Span label = new Span(labelText);
        label.getStyle().set("font-weight", "500");
        label.getStyle().set("font-size", "var(--lumo-font-size-s)");

        Button copyButton = new Button(new Icon(VaadinIcon.COPY));
        copyButton.setTooltipText("Copy to clipboard");
        copyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        copyButton.addClickListener(e -> {
            String value = textArea.getValue();
            if (StringUtils.hasText(value)) {
                UI.getCurrent().getPage().executeJs(
                        "navigator.clipboard.writeText($0).catch(e => console.error('Copy failed:', e));",
                        value
                );
                Notification.show("Copied to clipboard", 6000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("Nothing to copy", 6000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
        });

        header.add(label, copyButton);
        header.expand(label);

        textArea.setWidthFull();
        container.add(header, textArea);
        return container;
    }
}