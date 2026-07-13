package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One entry in the Activity tab's vertical timeline: a device-icon dot plus
 * the sign-in title, browser/device/IP meta line, and absolute+relative time.
 */
class ConnectionTimelineItem extends Div {

    ConnectionTimelineItem(ConnectionTrackingDto c) {
        addClassName("profile-timeline-item");

        Div dot = new Div();
        dot.addClassName("profile-timeline-dot");
        dot.add(ProfileFormatUtils.deviceIcon(c.getDevice()).create());

        Div body = new Div();
        body.addClassName("profile-timeline-body");

        Span titleSpan = new Span(I18n.t("profile.activity.signin",
                c.getLogApp() != null && !c.getLogApp().isBlank() ? c.getLogApp() : I18n.t("profile.history.unknown.app")));
        titleSpan.addClassName("profile-timeline-title");

        String metaLine = Stream.of(c.getBrowser(), c.getDevice(), c.getIpAddress())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span meta = new Span(metaLine);
        meta.addClassName("profile-timeline-meta");

        Span time = new Span(ProfileFormatUtils.formatAbsoluteAndRelative(c.getLoginDate()));
        time.addClassName("profile-timeline-time");

        body.add(titleSpan, meta, time);
        add(dot, body);
    }
}
