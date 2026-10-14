package eu.isygoit.ui.common.profile;

import com.storedobject.chart.*;
import eu.isygoit.dto.data.DailyLoginCountDto;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Column chart showing the number of tracked logins per day for the last
 * {@code days} days. Days with no login are rendered as zero so the timeline
 * stays continuous.
 *
 * <p>The chart instance is intended to be reused: construct it once and call
 * {@link #update(List, int)} whenever the window (7/30 days) changes, instead
 * of tearing down and recreating the component.</p>
 */
class LoginActivityChart extends SOChart {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final Color BAR_COLOR = new Color("#5B6EF5");

    private final CategoryData categories = new CategoryData();
    private final Data values = new Data();
    private final XAxis xAxis = new XAxis(categories);

    /**
     * Number of days currently displayed (7 or 30 by convention).
     */
    private int windowDays;

    LoginActivityChart(List<DailyLoginCountDto> history, int days) {
        addClassName("profile-login-activity-chart");
        setHeight("300px");
        setWidthFull();
        disableDefaultLegend();
        getDefaultTooltip().setType(Tooltip.Type.Axis);

        xAxis.getPointer(true).setType(Axis.PointerType.CROSS_HAIR);

        YAxis yAxis = new YAxis(DataType.NUMBER);
        yAxis.setName(I18n.t("profile.connections.chart.yAxis"));
        yAxis.setMin(0);
        yAxis.getLabel(true).setFormatterFunction("return Math.round(value);");

        RectangularCoordinate coordinate = new RectangularCoordinate(xAxis, yAxis);
        BarChart series = new BarChart(categories, values);
        series.setName(I18n.t("profile.connections.chart.series"));
        series.setColors(BAR_COLOR);
        series.getItemStyle(true).getBorder(true).setRadius(4);
        series.plotOn(coordinate);

        add(coordinate, series);
        update(history, days);
    }

    /**
     * Recompute the buckets for the last {@code days} days from the server's daily counts (which may cover a
     * longer period) and update the existing chart.
     */
    void update(List<DailyLoginCountDto> history, int days) {
        this.windowDays = days;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);

        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (DailyLoginCountDto daily : history) {
            LocalDate day = daily.getDate();
            if (day != null && !day.isBefore(from) && !day.isAfter(today)) {
                counts.merge(day, daily.getCount(), Long::sum);
            }
        }

        List<String> labels = new ArrayList<>(days);
        List<Number> dailyValues = new ArrayList<>(days);
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            labels.add(d.format(DAY_LABEL));
            dailyValues.add(counts.getOrDefault(d, 0L));
        }

        categories.clear();
        categories.addAll(labels);
        values.clear();
        values.addAll(dailyValues);
        xAxis.getLabel(true).setInterval(days <= 7 ? 0 : (int) Math.ceil(days / 10.0) - 1);

        if (isAttached()) {
            try {
                update(false);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to update login activity chart", e);
            }
        }
    }

    int getWindowDays() {
        return windowDays;
    }
}
