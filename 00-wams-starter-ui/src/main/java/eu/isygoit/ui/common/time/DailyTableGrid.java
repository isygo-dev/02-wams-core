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
 * Supports any {@code stepMinutes} (5, 10, 15, 30, 60…): row heights scale
 * proportionally so the day height stays roughly constant.
 */
@CssImport("./styles/scss/time-grid.scss")
public class DailyTableGrid<E extends DayTimeSlot> extends VerticalLayout {

    private static final String HEX_RE = "^#([A-Fa-f0-9]{6})$";
    private static final int REFERENCE_STEP_MINUTES = 30;

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
        gridContainer.getStyle().set("--tt-step-factor",
                String.valueOf(step / (double) REFERENCE_STEP_MINUTES));
        add(gridContainer);
    }

    private static <T, R> R safeApply(Function<T, R> fn, T value, R fallback) {
        if (fn == null) return fallback;
        try {
            R result = fn.apply(value);
            return result != null ? result : fallback;
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static String lighten(String hex, double amount) {
        if (hex == null || !hex.matches(HEX_RE)) return null;
        double k = Math.max(0.0, Math.min(1.0, amount));
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        r = (int) Math.round(r + (255 - r) * k);
        g = (int) Math.round(g + (255 - g) * k);
        b = (int) Math.round(b + (255 - b) * k);
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private String buildDayHeaderLabel() {
        String dayName = I18n.t("timetable.day." + currentDate.getDayOfWeek().name());
        String monthName = I18n.t("timetable.month." + currentDate.getMonth().name());
        return dayName + " " + currentDate.getDayOfMonth() + " " + monthName;
    }

    public void setDate(LocalDate date, List<E> events) {
        this.currentDate = date;
        this.events = (events != null) ? events : Collections.emptyList();
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();

        List<E> dayEvents = events.stream()
                .sorted(Comparator.comparing(e -> config.getStartTimeExtractor().apply(e)))
                .collect(Collectors.toList());

        if (dayEvents.isEmpty() && events.isEmpty()) {
            Div empty = new Div();
            empty.setText(I18n.t("calendar.grid.empty.day"));
            empty.addClassName("calendar-day-no-events");
            gridContainer.add(empty);
            return;
        }

        int totalRows = timeSlots.size();
        StringBuilder rowTemplate = new StringBuilder("auto ");
        for (int i = 0; i < totalRows - 1; i++) {
            rowTemplate.append("var(--tt-row-step) ");
        }
        rowTemplate.append("var(--tt-row-end)");
        gridContainer.addClassName("timetable-grid--daily");
        gridContainer.getStyle().set("grid-template-rows", rowTemplate.toString().trim());

        Div corner = new Div();
        corner.addClassName("timetable-grid-header");
        placeInGrid(corner, 1, 1, 1, 1);
        gridContainer.add(corner);

        Span dayLabel = new Span(buildDayHeaderLabel());
        dayLabel.addClassName("timetable-grid-header");
        dayLabel.addClassName("timetable-grid-header-day");
        dayLabel.addClassName("text-center");
        dayLabel.addClassName("font-semibold");
        if (currentDate.equals(LocalDate.now())) {
            dayLabel.addClassName("timetable-grid-header-day--today");
        }
        placeInGrid(dayLabel, 1, 2, 1, 1);
        gridContainer.add(dayLabel);

        List<LocalTime> bookableStarts = timeSlots.subList(0, timeSlots.size() - 1);

        Map<Integer, E> slotMap = new LinkedHashMap<>();
        Map<Integer, Integer> spanMap = new HashMap<>();
        Set<String> coveredRows = new HashSet<>();

        for (E evt : dayEvents) {
            LocalTime start = config.getStartTimeExtractor().apply(evt);
            if (start == null) continue;

            int rowIndex = rowIndexContaining(start, bookableStarts);
            if (rowIndex < 0) continue;

            LocalTime snappedStart = bookableStarts.get(rowIndex);
            int span = spanFor(evt, snappedStart);

            slotMap.put(rowIndex, evt);
            spanMap.put(rowIndex, span);
            for (int r = rowIndex; r < rowIndex + span; r++) {
                coveredRows.add("day-" + r);
            }
        }

        int step = config.getStepMinutes();
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
            if (start.getMinute() == 0) {
                cell.addClassName("hour-full");
            } else if (step < 60 && start.getMinute() % 30 == 0) {
                cell.addClassName("hour-half");
            } else {
                cell.addClassName("hour-quarter");
            }
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

        for (Map.Entry<Integer, E> entry : slotMap.entrySet()) {
            int rowIndex = entry.getKey();
            E evt = entry.getValue();
            int span = spanMap.get(rowIndex);
            Component chip = buildSlotChip(evt, span);
            placeInGrid(chip, rowIndex + 2, 2, span, 1);
            gridContainer.add(chip);
        }

        if (config.isShowNowIndicator()) {
            installNowIndicator();
        }
    }

    private int rowIndexContaining(LocalTime start, List<LocalTime> sortedStarts) {
        int pos = Collections.binarySearch(sortedStarts, start);
        if (pos >= 0) return pos;
        int insertionPoint = -pos - 1;
        if (insertionPoint == 0) return -1;
        return insertionPoint - 1;
    }

    private int spanFor(E evt, LocalTime snappedStart) {
        LocalTime end = config.getEndTimeExtractor().apply(evt);
        if (end == null) return 1;
        long durationMinutes = Duration.between(snappedStart, end).toMinutes();
        if (durationMinutes <= 0) return 1;
        return Math.max(1, (int) Math.ceil(durationMinutes / (double) config.getStepMinutes()));
    }

    private Component buildSlotChip(E evt, int span) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.addClassName("timetable-slot-content");
        content.addClassName("timetable-slot-chip");
        if (span > 1) content.addClassName("timetable-grid-cell--multi-hour");

        String explicit = safeApply(config.getColorExtractor(), evt, null);
        if (explicit != null && explicit.matches(HEX_RE)) {
            content.getStyle().set("--slot-accent", explicit);
            String bg = lighten(explicit, 0.85);
            content.getStyle().set("--slot-bg", bg != null ? bg : explicit);
        } else {
            Object key = config.getCategoryKeyExtractor() != null
                    ? config.getCategoryKeyExtractor().apply(evt) : null;
            List<String[]> palette = config.getPalette();
            int idx = key != null ? Math.floorMod(key.hashCode(), palette.size()) : 0;
            String[] colors = palette.get(idx);
            content.getStyle().set("--slot-bg", colors[0]);
            content.getStyle().set("--slot-accent", colors[1]);
        }

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

    private String buildTooltipText(E evt) {
        if (config.getTooltipExtractor() != null) {
            String custom = safeApply(config.getTooltipExtractor(), evt, null);
            if (custom != null && !custom.isBlank()) return custom;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(safeApply(config.getTitleExtractor(), evt, ""));

        String desc = safeApply(config.getDescriptionExtractor(), evt, null);
        if (desc != null && !desc.isBlank()) sb.append("\n").append(desc);

        if (currentDate != null) {
            sb.append("\n").append(currentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }

        LocalTime start = safeApply(config.getStartTimeExtractor(), evt, LocalTime.MIDNIGHT);
        LocalTime end = safeApply(config.getEndTimeExtractor(), evt, LocalTime.MIDNIGHT);
        sb.append("\n").append(start).append(" – ").append(end);

        String owner = safeApply(config.getOwnerExtractor(), evt, null);
        if (owner != null && !owner.isBlank()) {
            sb.append("\n").append(I18n.t("calendar.grid.owner")).append(" ").append(owner);
        }
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