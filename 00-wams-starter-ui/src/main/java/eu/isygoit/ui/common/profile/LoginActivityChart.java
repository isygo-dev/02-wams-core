package eu.isygoit.ui.common.profile;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import eu.isygoit.dto.data.ConnectionTrackingDto;
import eu.isygoit.i18n.I18n;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Column chart showing the number of tracked logins per day for the last
 * {@code days} days. Days with no login are rendered as zero so the timeline
 * stays continuous.
 *
 * <p>The chart instance is intended to be reused: construct it once and call
 * {@link #update(List, int)} whenever the window (7/30 days) changes, instead
 * of tearing down and recreating the component.</p>
 */
class LoginActivityChart extends Chart {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final SolidColor BAR_COLOR = new SolidColor("#5B6EF5");

    /**
     * Number of days currently displayed (7 or 30 by convention).
     */
    private int windowDays;

    LoginActivityChart(List<ConnectionTrackingDto> history, int days) {
        super(ChartType.COLUMN);

        addClassName("profile-login-activity-chart");
        setHeight("300px");
        setWidthFull();

        Configuration cfg = getConfiguration();
        cfg.getCredits().setEnabled(false);   // hide "Highcharts.com" link
        cfg.getLegend().setEnabled(false);    // single series → no legend
        cfg.getExporting().setEnabled(false); // hide the hamburger menu

        // Axes are configured once; update() only swaps their data.
        XAxis xAxis = new XAxis();
        xAxis.setCrosshair(new Crosshair());
        cfg.addxAxis(xAxis);

        YAxis yAxis = new YAxis();
        yAxis.setTitle(I18n.t("profile.connections.chart.yAxis"));
        yAxis.setMin(0);
        yAxis.setAllowDecimals(false);
        cfg.addyAxis(yAxis);

        PlotOptionsColumn plot = new PlotOptionsColumn();
        plot.setColor(BAR_COLOR);
        plot.setBorderRadius(4);
        plot.setDataLabels(new DataLabels(false));

        ListSeries series = new ListSeries(I18n.t("profile.connections.chart.series"), new ArrayList<Number>());
        series.setPlotOptions(plot);
        cfg.addSeries(series);

        Tooltip tooltip = new Tooltip();
        tooltip.setShared(true);
        cfg.setTooltip(tooltip);

        update(history, days);
    }

    /**
     * Adapt this to the actual date accessor on {@link ConnectionTrackingDto}.
     * The DTO is assumed to expose a login timestamp (e.g. {@code getLoginDate()}
     * returning {@code java.util.Date} / {@code Instant} / {@code LocalDateTime}).
     */
    private static LocalDate toLocalDate(ConnectionTrackingDto dto) {
        if (dto == null || dto.getLoginDate() == null) {
            return null;
        }
        Object login = dto.getLoginDate();
        if (login instanceof LocalDate ld) {
            return ld;
        }
        if (login instanceof java.time.LocalDateTime ldt) {
            return ldt.toLocalDate();
        }
        if (login instanceof java.time.Instant inst) {
            return inst.atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (login instanceof java.util.Date date) {
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    /**
     * Recompute the buckets for {@code days} and push them into the existing
     * series / axis without recreating the chart.
     */
    void update(List<ConnectionTrackingDto> history, int days) {
        this.windowDays = days;

        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);

        Map<LocalDate, Long> counts = history.stream()
                .map(LoginActivityChart::toLocalDate)
                .filter(d -> d != null && !d.isBefore(from) && !d.isAfter(today))
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        LinkedHashMap::new,
                        Collectors.counting()));

        List<String> categories = new ArrayList<>(days);
        List<Number> values = new ArrayList<>(days);
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            categories.add(d.format(DAY_LABEL));
            values.add(counts.getOrDefault(d, 0L));
        }

        Configuration cfg = getConfiguration();
        XAxis xAxis = cfg.getxAxis();
        xAxis.setCategories(categories.toArray(new String[0]));
        xAxis.setTickInterval(days <= 7 ? 1 : (int) Math.ceil(days / 10.0));

        ListSeries series = (ListSeries) cfg.getSeries().get(0);
        series.setData(values);

        drawChart();
    }

    int getWindowDays() {
        return windowDays;
    }
}