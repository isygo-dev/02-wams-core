package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Centered icon + message shown wherever a profile panel has no data yet
 * (no connection history, no activity in the selected chart window, ...).
 */
class ProfileEmptyState extends Div {

    ProfileEmptyState(String message) {
        addClassName("profile-empty-state");
        Icon icon = VaadinIcon.INBOX.create();
        icon.addClassName("profile-empty-state__icon");
        add(icon, new Span(message));
    }
}
