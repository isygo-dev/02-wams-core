package eu.isygoit.ui.common.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import eu.isygoit.i18n.I18n;
import eu.isygoit.util.SecurityUtils;

public class ManagementVerticalView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public final void beforeEnter(BeforeEnterEvent event) {
        String currentPath = event.getLocation().getPathWithQueryParameters(); // Better: includes query params if any
        if (!SecurityUtils.isUserLoggedIn()) {
            UI.getCurrent().getPage().setLocation("login?redirect=" + java.net.URLEncoder.encode(currentPath, java.nio.charset.StandardCharsets.UTF_8));
        } else {
            SecurityUtils.storeRedirect(currentPath);
        }
    }

    public static Button createCopyButton(VaadinIcon icon, String textToCopy, String tooltip) {
        Button btn = new Button(new Icon(icon));
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        btn.setTooltipText(tooltip);
        btn.setWidth("20px");
        btn.setHeight("20px");
        btn.addClickListener(e -> copyToClipboard(textToCopy, I18n.t("kms.dashboard.copied", textToCopy)));
        return btn;
    }

    public static void copyToClipboard(String text, String notificationText) {
        UI.getCurrent().getPage().executeJs(
                "navigator.clipboard.writeText($0).then(() => { $0.dispatchEvent(new Event('copy-success')); }).catch(() => { $0.dispatchEvent(new Event('copy-error')); });",
                text
        );
        Notification.show(notificationText, 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }
}