package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * One row in the Connections tab's "Recent Sessions" list: device icon,
 * browser/device summary, IP + relative time, and an application chip.
 */
class ConnectionCard extends Div {

    ConnectionCard(ConnectionTrackingDto c) {
        addClassName("profile-connection-item");

        Div iconCircle = new Div();
        iconCircle.addClassName("profile-connection-icon");
        iconCircle.add(ProfileFormatUtils.deviceIcon(c.getDevice()).create());

        Div text = new Div();
        text.addClassName("profile-connection-text");

        String primaryLine = Stream.of(c.getBrowser(), c.getDevice())
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span primary = new Span(!primaryLine.isBlank() ? primaryLine : I18n.t("profile.history.unknown.browser"));
        primary.addClassName("profile-connection-primary");

        String metaLine = Stream.of(c.getIpAddress(), ProfileFormatUtils.formatRelativeTime(c.getLoginDate()))
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" · "));
        Span meta = new Span(metaLine);
        meta.addClassName("profile-connection-meta");

        text.add(primary, meta);

        Span appChip = new Span(c.getLogApp() != null && !c.getLogApp().isBlank() ? c.getLogApp() : I18n.t("profile.history.unknown.app"));
        appChip.addClassName("wams-chip");
        appChip.addClassName("wams-chip--info");
        appChip.addClassName("profile-connection-chip");

        add(iconCircle, text, appChip);
    }
}
