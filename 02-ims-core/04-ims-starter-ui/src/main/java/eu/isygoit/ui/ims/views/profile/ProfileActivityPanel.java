package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.util.List;

/**
 * Activity tab: the full connection history as a chronological timeline
 * (newest first), one {@link ConnectionTimelineItem} per login session.
 */
class ProfileActivityPanel extends VerticalLayout {

    ProfileActivityPanel(List<ConnectionTrackingDto> history) {
        setPadding(true);
        setSpacing(true);
        setWidthFull();
        addClassName("profile-tab-panel");

        Div card = new Div();
        card.addClassName("profile-section-card");

        H3 title = new H3(I18n.t("profile.section.activity"));
        title.addClassName("section-title");
        card.add(title);

        List<ConnectionTrackingDto> recentFirst = ProfileFormatUtils.recentFirst(history);

        if (recentFirst.isEmpty()) {
            card.add(new ProfileEmptyState(I18n.t("profile.history.empty")));
        } else {
            Div timeline = new Div();
            timeline.addClassName("profile-timeline");
            timeline.getElement().setAttribute("aria-label", I18n.t("profile.section.activity"));
            recentFirst.forEach(c -> timeline.add(new ConnectionTimelineItem(c)));
            card.add(timeline);
        }

        add(card);
    }
}
