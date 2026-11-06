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

    private static final String COPY_JS =
            "const t = $0;"
                    + "if (navigator.clipboard && window.isSecureContext) { navigator.clipboard.writeText(t); }"
                    + "else { const a = document.createElement('textarea'); a.value = t;"
                    + "a.style.position = 'fixed'; a.style.opacity = '0'; document.body.appendChild(a);"
                    + "a.select(); document.execCommand('copy'); a.remove(); }";

    public ClipboardCopyButton(String textToCopy) {
        this(textToCopy, I18n.t("common.action.copy.tooltip"));
    }

    public ClipboardCopyButton(String textToCopy, String tooltip) {
        this(textToCopy, tooltip, I18n.t("common.action.copy.success"));
    }

    public ClipboardCopyButton(String textToCopy, String tooltip, String successMessage) {
        super(VaadinIcon.COPY.create());
        addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        addClassName("wams-copy-button");
        setTooltipText(tooltip);
        setAriaLabel(tooltip);
        addClickListener(e -> copy(textToCopy, successMessage));
    }

    private void copy(String text, String successMessage) {
        if (text == null || text.isBlank()) {
            return;
        }
        UI.getCurrent().getPage().executeJs(COPY_JS, text);
        Notification.show(successMessage, 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}
