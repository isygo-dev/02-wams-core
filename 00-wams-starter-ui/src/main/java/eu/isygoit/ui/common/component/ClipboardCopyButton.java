package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import eu.isygoit.i18n.I18n;

/**
 * Small inline button placed next to a long detail-view value (an ID, hash,
 * key, path, URL, ...) that copies that value to the clipboard on click.
 * Shared across every module's "…DetailsViewDialog" so long values never have
 * to be manually selected/dragged.
 */
public class ClipboardCopyButton extends Button {

    public ClipboardCopyButton(String textToCopy) {
        this(textToCopy, I18n.t("common.action.copy.tooltip"));
    }

    public ClipboardCopyButton(String textToCopy, String tooltip) {
        super(VaadinIcon.COPY.create());
        addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        addClassName("wams-copy-button");
        setTooltipText(tooltip);
        addClickListener(e -> copy(textToCopy));
    }

    private void copy(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text);
        Notification.show(I18n.t("common.action.copy.success"), 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
