package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dependency-free login-activity chart for the Connections tab: buckets
 * {@link ConnectionTrackingDto} logins into a daily count for the requested
 * window (7 or 30 days) and renders it as a plain inline SVG bar chart, so no
 * charting library needs to be added to the project for one small graph.
 */
class LoginActivityChart extends Div {

    private static final DateTimeFormatter CHART_WEEKDAY_FMT = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter CHART_DAY_FMT = DateTimeFormatter.ofPattern("dd MMM");

    LoginActivityChart(List<ConnectionTrackingDto> history, int days) {
        addClassName("profile-chart-svg-holder");
        add(render(history, days));
    }

    private Component render(List<ConnectionTrackingDto> history, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int i = days - 1; i >= 0; i--) {
            counts.put(today.minusDays(i), 0L);
        }
        for (ConnectionTrackingDto c : history) {
            if (c.getLoginDate() == null) {
                continue;
            }
            LocalDate day = c.getLoginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            counts.merge(day, 1L, Long::sum);
        }
        // Discard buckets outside the requested window (merge above may have
        // added days beyond it if history contains future-dated test data).
        counts.keySet().removeIf(d -> d.isBefore(today.minusDays(days - 1)) || d.isAfter(today));

        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        if (total == 0) {
            return new ProfileEmptyState(I18n.t("profile.connections.chart.empty"));
        }

        int slot = days <= 7 ? 40 : 12;
        int barWidth = days <= 7 ? 22 : 8;
        int chartHeight = 120;
        int labelHeight = 22;
        int width = counts.size() * slot;
        int totalHeight = chartHeight + labelHeight;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 ").append(width).append(' ').append(totalHeight)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"")
                .append(I18n.t("profile.connections.chart.aria", total, days))
                .append("\" preserveAspectRatio=\"none\" class=\"profile-chart-svg\">");

        int x = 0;
        int index = 0;
        int lastIndex = counts.size() - 1;
        for (Map.Entry<LocalDate, Long> entry : counts.entrySet()) {
            long count = entry.getValue();
            int barHeight = max == 0 ? 0 : (int) Math.round((count / (double) max) * (chartHeight - 8));
            if (count > 0) {
                barHeight = Math.max(barHeight, 3);
            }
            int barX = x + (slot - barWidth) / 2;
            int barY = chartHeight - barHeight;

            svg.append("<rect class=\"profile-chart-bar\" x=\"").append(barX).append("\" y=\"").append(barY)
                    .append("\" width=\"").append(barWidth).append("\" height=\"").append(barHeight)
                    .append("\" rx=\"2\"><title>").append(entry.getKey()).append(": ").append(count)
                    .append("</title></rect>");

            boolean showLabel = days <= 7 || index % 5 == 0 || index == lastIndex;
            if (showLabel) {
                String label = days <= 7 ? entry.getKey().format(CHART_WEEKDAY_FMT) : entry.getKey().format(CHART_DAY_FMT);
                svg.append("<text class=\"profile-chart-label\" x=\"").append(x + slot / 2.0).append("\" y=\"")
                        .append(chartHeight + 15).append("\" text-anchor=\"middle\">").append(label).append("</text>");
            }
            x += slot;
            index++;
        }
        svg.append("</svg>");

        return new Html("<div class=\"profile-chart-svg-wrapper\">" + svg + "</div>");
    }
}
