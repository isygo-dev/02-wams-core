package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import eu.isygoit.dto.common.DayTimeSlot;
import eu.isygoit.i18n.I18n;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Daily timetable grid – one column for the day, time labels on the left.
 * Matches the weekly timetable in styling and behaviour.
 */
@CssImport("./styles/scss/time-grid.scss")
public class DailyTableGrid<E extends DayTimeSlot> extends VerticalLayout {

    private final DailyTableGridConfig<E> config;
    private final Div gridContainer = new Div();
    private final List<LocalTime> timeSlots = new ArrayList<>();
    private LocalDate currentDate;
    private List<E> events;

    public DailyTableGrid(DailyTableGridConfig<E> config) {
        this.config = config;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("timetable-grid-wrapper");
        addClassName("calendar-view-container");

        // Build time slots
        int startHour = config.getStartHour();
        int endHour = config.getEndHour();
        int step = config.getStepMinutes();
        for (int h = startHour; h < endHour; h++) {
            for (int m = 0; m < 60; m += step) {
                timeSlots.add(LocalTime.of(h, m));
            }
        }
        timeSlots.add(LocalTime.of(endHour, 0));

        gridContainer.setWidthFull();
        gridContainer.addClassName("timetable-grid");
        add(gridContainer);
    }

    /**
     * Null-safe helper — invokes the extractor only if it is non-null.
     */
    private static <T, R> R safeApply(Function<T, R> fn, T value, R fallback) {
        if (fn == null) return fallback;
        try {
            R result = fn.apply(value);
            return result != null ? result : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    public void setDate(LocalDate date, List<E> events) {
        this.currentDate = date;
        this.events = (events != null) ? events : Collections.emptyList();
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();

        List<E> dayEvents = events.stream()
                .filter(e -> true)
                .sorted(Comparator.comparing(e -> config.getStartTimeExtractor().apply(e)))
                .collect(Collectors.toList());

        if (dayEvents.isEmpty() && events.isEmpty()) {
            Div empty = new Div();
            empty.setText(I18n.t("calendar.grid.empty.day"));
            empty.addClassName("calendar-day-no-events");
            gridContainer.add(empty);
            return;
        }

        // ---- Grid template ----
        int totalRows = timeSlots.size();
        StringBuilder rowTemplate = new StringBuilder("auto ");
        for (int i = 0; i < totalRows - 1; i++) {
            rowTemplate.append(timeSlots.get(i).getMinute() == 0 ? "var(--tt-row-full) " : "var(--tt-row-half) ");
        }
        rowTemplate.append("var(--tt-row-end)");
        gridContainer.addClassName("timetable-grid--daily");
        gridContainer.getStyle().set("grid-template-rows", rowTemplate.toString().trim());

        // ---- Header row ----
        Div corner = new Div();
        corner.addClassName("timetable-grid-header");
        placeInGrid(corner, 1, 1, 1, 1);
        gridContainer.add(corner);

        Span dayLabel = new Span(currentDate.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)));
        dayLabel.addClassName("timetable-grid-header");
        dayLabel.addClassName("timetable-grid-header-day");
        dayLabel.addClassName("text-center");
        dayLabel.addClassName("font-semibold");
        if (currentDate.equals(LocalDate.now())) {
            dayLabel.addClassName("timetable-grid-header-day--today");
        }
        placeInGrid(dayLabel, 1, 2, 1, 1);
        gridContainer.add(dayLabel);

        // ---- Map events to rows ----
        List<LocalTime> bookableStarts = timeSlots.subList(0, timeSlots.size() - 1);
        Map<LocalTime, Integer> rowIndexByTime = new HashMap<>();
        for (int i = 0; i < bookableStarts.size(); i++) {
            rowIndexByTime.put(bookableStarts.get(i), i);
        }

        Map<Integer, E> slotMap = new LinkedHashMap<>();
        Set<String> coveredRows = new HashSet<>();

        for (E evt : dayEvents) {
            LocalTime start = config.getStartTimeExtractor().apply(evt);
            Integer rowIndex = rowIndexByTime.get(start);
            if (rowIndex != null) {
                slotMap.put(rowIndex, evt);
                int span = spanFor(evt);
                for (int r = rowIndex; r < rowIndex + span; r++) {
                    coveredRows.add("day-" + r);
                }
            }
        }

        // ---- Time labels and background cells ----
        for (int rowIndex = 0; rowIndex < bookableStarts.size(); rowIndex++) {
            LocalTime start = bookableStarts.get(rowIndex);

            Span timeLabel = new Span(start.getMinute() == 0 ? formatHour(start) : "");
            timeLabel.addClassName("timetable-time-label");
            timeLabel.addClassName("text-xs");
            timeLabel.addClassName("text-secondary");
            placeInGrid(timeLabel, rowIndex + 2, 1, 1, 1);
            gridContainer.add(timeLabel);

            Div cell = new Div();
            cell.addClassName("timetable-bg-cell");
            cell.addClassName(start.getMinute() == 0 ? "hour-full" : "hour-half");
            if (currentDate.equals(LocalDate.now())) {
                cell.addClassName("timetable-grid-col-today");
            }
            placeInGrid(cell, rowIndex + 2, 2, 1, 1);

            boolean isCovered = coveredRows.contains("day-" + rowIndex);
            if (!isCovered && config.getOnEmptyCellClick() != null) {
                cell.addClassName("timetable-grid-cell--empty");
                LocalTime endTime = (rowIndex + 1 < bookableStarts.size())
                        ? bookableStarts.get(rowIndex + 1)
                        : LocalTime.of(config.getEndHour(), 0);
                cell.addClickListener(e -> config.getOnEmptyCellClick().accept(start, endTime));
                Span plus = new Span("+");
                plus.addClassName("timetable-empty-plus");
                cell.add(plus);
            }
            gridContainer.add(cell);
        }

        // ---- Event chips ----
        for (Map.Entry<Integer, E> entry : slotMap.entrySet()) {
            int rowIndex = entry.getKey();
            E evt = entry.getValue();
            int span = spanFor(evt);
            Component chip = buildSlotChip(evt, span);
            placeInGrid(chip, rowIndex + 2, 2, span, 1);
            gridContainer.add(chip);
        }

        // ---- Now indicator ----
        if (config.isShowNowIndicator()) {
            installNowIndicator();
        }
    }

    private int spanFor(E evt) {
        LocalTime start = config.getStartTimeExtractor().apply(evt);
        LocalTime end = config.getEndTimeExtractor().apply(evt);
        int durationMinutes = (int) Duration.between(start, end).toMinutes();
        return Math.max(1, (int) Math.round((double) durationMinutes / config.getStepMinutes()));
    }

    private Component buildSlotChip(E evt, int span) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.addClassName("timetable-slot-content");
        content.addClassName("timetable-slot-chip");
        if (span > 1) {
            content.addClassName("timetable-grid-cell--multi-hour");
        }

        Object key = config.getCategoryKeyExtractor() != null
                ? config.getCategoryKeyExtractor().apply(evt) : null;
        List<String[]> palette = config.getPalette();
        int idx = key != null ? Math.floorMod(key.hashCode(), palette.size()) : 0;
        String[] colors = palette.get(idx);
        content.getStyle().set("--slot-bg", colors[0]);
        content.getStyle().set("--slot-accent", colors[1]);

        if (config.getSlotContentPopulator() != null) {
            config.getSlotContentPopulator().accept(content, evt);
        } else {
            Span title = new Span(safeApply(config.getTitleExtractor(), evt, ""));
            title.addClassName("timetable-slot-title");
            content.add(title);
            Span meta = new Span(safeApply(config.getLocationExtractor(), evt, ""));
            meta.addClassName("timetable-slot-meta");
            content.add(meta);
        }

        if (config.getIsEditable().test(evt)) {
            MenuBar menu = buildActionsMenu(evt);
            content.add(menu);
        }

        String tooltip = buildTooltipText(evt);
        Tooltip.forComponent(content).setText(tooltip);

        if (config.getOnEventClick() != null) {
            content.addClickListener(e -> config.getOnEventClick().accept(evt));
        }
        return content;
    }

    private MenuBar buildActionsMenu(E evt) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE,
                MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_SMALL);
        menuBar.addClassName("timetable-slot-actions");
        menuBar.getElement().executeJs("this.addEventListener('click', (e) => e.stopPropagation());");

        MenuItem rootItem = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        SubMenu subMenu = rootItem.getSubMenu();

        if (config.getOnEventEdit() != null) {
            subMenu.addItem(I18n.t("calendar.grid.edit"), e -> config.getOnEventEdit().accept(evt));
        }
        if (config.getOnEventDelete() != null) {
            MenuItem deleteItem = subMenu.addItem(I18n.t("calendar.grid.delete"),
                    e -> config.getOnEventDelete().accept(evt));
            deleteItem.addClassName("timetable-delete-menu-item");
        }
        return menuBar;
    }

    /**
     * Builds a tooltip from the configured extractors.
     * Every extractor access is null-safe so a config that forgot to set an
     * optional extractor will not throw NPE here.
     */
    private String buildTooltipText(E evt) {
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append(safeApply(config.getTitleExtractor(), evt, ""));

        // Description
        String desc = safeApply(config.getDescriptionExtractor(), evt, null);
        if (desc != null && !desc.isBlank()) {
            sb.append("\n").append(desc);
        }

        // Date
        if (currentDate != null) {
            sb.append("\n").append(currentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        // Time range
        LocalTime start = safeApply(config.getStartTimeExtractor(), evt, LocalTime.MIDNIGHT);
        LocalTime end = safeApply(config.getEndTimeExtractor(), evt, LocalTime.MIDNIGHT);
        sb.append("\n").append(start).append(" – ").append(end);

        // Owner
        String owner = safeApply(config.getOwnerExtractor(), evt, null);
        if (owner != null && !owner.isBlank()) {
            sb.append("\n").append(I18n.t("calendar.grid.owner")).append(" ").append(owner);
        }

        // Location
        String location = safeApply(config.getLocationExtractor(), evt, null);
        if (location != null && !location.isBlank()) {
            sb.append("\n").append(I18n.t("calendar.grid.location")).append(" ").append(location);
        }

        return sb.toString();
    }

    private String formatHour(LocalTime time) {
        return time.getHour() + "h";
    }

    private void placeInGrid(Component component, int rowStart, int colStart, int rowSpan, int colSpan) {
        component.getStyle().set("grid-row", rowStart + " / span " + rowSpan);
        component.getStyle().set("grid-column", colStart + " / span " + colSpan);
    }

    private void installNowIndicator() {
        int startMinutes = config.getStartHour() * 60;
        int endMinutes = config.getEndHour() * 60;
        gridContainer.getElement().executeJs(
                "const el = this; if (el._nowInterval) clearInterval(el._nowInterval);" +
                        "const update = () => {" +
                        "  const cells = el.querySelectorAll('.timetable-bg-cell');" +
                        "  if (!cells.length) return;" +
                        "  const now = new Date(); const totalMin = now.getHours()*60 + now.getMinutes();" +
                        "  if (totalMin < " + startMinutes + " || totalMin > " + endMinutes + ") {" +
                        "    let ind = el.querySelector('.timetable-now-indicator'); if(ind) ind.style.display = 'none'; return;" +
                        "  }" +
                        "  const firstCell = cells[0];" +
                        "  let totalHeight = 0; cells.forEach(c => totalHeight += c.offsetHeight);" +
                        "  const fraction = (totalMin - " + startMinutes + ") / (" + endMinutes + " - " + startMinutes + ");" +
                        "  const top = firstCell.offsetTop + fraction * totalHeight;" +
                        "  const rect = el.getBoundingClientRect();" +
                        "  const firstColRect = firstCell.getBoundingClientRect();" +
                        "  const colWidth = firstColRect.width;" +
                        "  const gutter = firstColRect.left - rect.left;" +
                        "  let ind = el.querySelector('.timetable-now-indicator');" +
                        "  if (!ind) { ind = document.createElement('div'); ind.className = 'timetable-now-indicator';" +
                        "    ind.innerHTML = '<span class=\"timetable-now-dot\"></span>'; el.appendChild(ind); }" +
                        "  ind.style.display = 'block';" +
                        "  ind.style.top = top + 'px';" +
                        "  ind.style.left = gutter + 'px';" +
                        "  ind.style.width = colWidth + 'px';" +
                        "}; update(); el._nowInterval = setInterval(update, 60000);"
        );
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (currentDate != null && events != null) {
            buildGrid();
        }
    }
}