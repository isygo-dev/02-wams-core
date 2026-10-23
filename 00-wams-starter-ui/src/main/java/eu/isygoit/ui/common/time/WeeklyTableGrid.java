package eu.isygoit.ui.common.time;

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
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.DayTimeSlot;
import eu.isygoit.i18n.I18n;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@CssImport("./styles/scss/time-grid.scss")
public class WeeklyTableGrid<E extends DayTimeSlot> extends VerticalLayout {

    private static final String HEX_RE = "^#([A-Fa-f0-9]{6})$";
    private static final int REFERENCE_STEP_MINUTES = 30;

    private final WeeklyTableGridConfig<E> config;
    private final Div gridContainer = new Div();
    private final List<LocalTime> timeSlots = new ArrayList<>();
    private List<E> slots = Collections.emptyList();
    private boolean editable = true;
    private LocalDate weekStartDate;

    public WeeklyTableGrid(WeeklyTableGridConfig<E> config) {
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
        // Publish the step factor. The actual row height is computed by the SCSS:
        //   --tt-row-step = calc(--tt-row-step-base * --tt-step-factor)
        // so the responsive breakpoints stay effective whatever the step.
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

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public void setItems(List<E> slots, boolean editable) {
        this.slots = slots != null ? slots : Collections.emptyList();
        this.editable = editable;
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();
        if (slots.isEmpty()) {
            Div empty = new Div();
            empty.setText(I18n.t("calendar.grid.empty.week"));
            empty.addClassName("calendar-day-no-events");
            gridContainer.add(empty);
            return;
        }

        List<DayOfWeek> days = config.getDaysOfWeek();
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        List<LocalTime> bookableStarts = timeSlots.subList(0, timeSlots.size() - 1);
        LocalTime endMarker = timeSlots.get(timeSlots.size() - 1);

        Map<DayOfWeek, Map<Integer, E>> slotsByDayRow = new HashMap<>();
        Map<DayOfWeek, Map<Integer, Integer>> spansByDayRow = new HashMap<>();
        Set<String> coveredRows = new HashSet<>();

        for (E slot : slots) {
            LocalTime start = config.getStartTimeExtractor().apply(slot);
            if (start == null) continue;

            int rowIndex = rowIndexContaining(start, bookableStarts);
            if (rowIndex < 0) continue;

            LocalTime snappedStart = bookableStarts.get(rowIndex);
            int span = spanFor(slot, snappedStart);

            DayOfWeek day = config.getDayOfWeekExtractor().apply(slot);
            slotsByDayRow.computeIfAbsent(day, d -> new HashMap<>()).put(rowIndex, slot);
            spansByDayRow.computeIfAbsent(day, d -> new HashMap<>()).put(rowIndex, span);
            for (int r = rowIndex; r < rowIndex + span; r++) {
                coveredRows.add(day.name() + "-" + r);
            }
        }

        // Row template: uniform `--tt-row-step` per row, `--tt-row-end` for the closing slot.
        StringBuilder rowTemplate = new StringBuilder("auto ");
        for (int i = 0; i < bookableStarts.size(); i++) {
            rowTemplate.append("var(--tt-row-step) ");
        }
        rowTemplate.append("var(--tt-row-end)");
        gridContainer.getStyle().set("grid-template-columns",
                "64px repeat(" + days.size() + ", minmax(0, 1fr))");
        gridContainer.getStyle().set("grid-template-rows", rowTemplate.toString().trim());

        buildHeader(today);

        for (int rowIndex = 0; rowIndex < bookableStarts.size(); rowIndex++) {
            LocalTime start = bookableStarts.get(rowIndex);
            buildTimeLabel(start, rowIndex, false);

            for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
                DayOfWeek day = days.get(dayIndex);
                boolean isStart = slotsByDayRow.containsKey(day) && slotsByDayRow.get(day).containsKey(rowIndex);
                boolean isCovered = coveredRows.contains(day.name() + "-" + rowIndex);

                buildBackgroundCell(day, dayIndex, rowIndex, start, today, isStart || isCovered);

                if (isStart) {
                    E slot = slotsByDayRow.get(day).get(rowIndex);
                    int span = spansByDayRow.get(day).get(rowIndex);
                    buildSlotChip(slot, dayIndex, rowIndex, span);
                }
            }
        }

        buildTimeLabel(endMarker, bookableStarts.size(), true);

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

    private int spanFor(E slot, LocalTime snappedStart) {
        LocalTime end = config.getEndTimeExtractor().apply(slot);
        if (end == null) return 1;
        long durationMinutes = Duration.between(snappedStart, end).toMinutes();
        if (durationMinutes <= 0) return 1;
        return Math.max(1, (int) Math.ceil(durationMinutes / (double) config.getStepMinutes()));
    }

    private String buildHeaderLabel(DayOfWeek day, int dayIndex) {
        String dayName = config.getDayLabelExtractor() != null
                ? config.getDayLabelExtractor().apply(day)
                : day.name();

        if (weekStartDate == null) return dayName;
        LocalDate cellDate = weekStartDate.plusDays(dayIndex);
        String monthName = I18n.t("timetable.month." + cellDate.getMonth().name());
        return dayName + " " + cellDate.getDayOfMonth() + " " + monthName;
    }

    private void buildHeader(DayOfWeek today) {
        Div corner = new Div();
        corner.addClassName("timetable-grid-header");
        placeInGrid(corner, 1, 1, 1, 1);
        gridContainer.add(corner);

        List<DayOfWeek> days = config.getDaysOfWeek();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            DayOfWeek day = days.get(dayIndex);

            Span dayLabel = new Span(buildHeaderLabel(day, dayIndex));
            dayLabel.addClassName("timetable-grid-header");
            dayLabel.addClassName("timetable-grid-header-day");
            dayLabel.addClassName(LumoUtility.TextAlignment.CENTER);
            dayLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
            if (config.getIsTodayPredicate().test(day)) {
                dayLabel.addClassName("timetable-grid-header-day--today");
            }
            placeInGrid(dayLabel, 1, dayIndex + 2, 1, 1);
            gridContainer.add(dayLabel);
        }
    }

    private void buildTimeLabel(LocalTime time, int rowIndex, boolean isClosingLabel) {
        Span timeLabel = new Span(time.getMinute() == 0 ? formatHour(time) : "");
        timeLabel.addClassName("timetable-time-label");
        if (isClosingLabel) timeLabel.addClassName("timetable-time-label--end");
        timeLabel.addClassName(LumoUtility.FontSize.XSMALL);
        timeLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        placeInGrid(timeLabel, rowIndex + 2, 1, 1, 1);
        gridContainer.add(timeLabel);
    }

    private void buildBackgroundCell(DayOfWeek day, int dayIndex, int rowIndex, LocalTime start,
                                     DayOfWeek today, boolean occupied) {
        Div cell = new Div();
        cell.addClassName("timetable-bg-cell");
        int step = config.getStepMinutes();
        if (start.getMinute() == 0) {
            cell.addClassName("hour-full");
        } else if (step < 60 && start.getMinute() % 30 == 0) {
            cell.addClassName("hour-half");
        } else {
            cell.addClassName("hour-quarter");
        }
        if (day == today) cell.addClassName("timetable-grid-col-today");
        placeInGrid(cell, rowIndex + 2, dayIndex + 2, 1, 1);

        if (!occupied && editable && config.getOnEmptyCellClick() != null) {
            cell.addClassName("timetable-grid-cell--empty");
            cell.addClickListener(e -> config.getOnEmptyCellClick().accept(day, start));
            Span plus = new Span("+");
            plus.addClassName("timetable-empty-plus");
            cell.add(plus);
        }
        gridContainer.add(cell);
    }

    private void buildSlotChip(E slot, int dayIndex, int rowIndex, int span) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.addClassName("timetable-slot-content");
        content.addClassName("timetable-slot-chip");
        if (span > 1) content.addClassName("timetable-grid-cell--multi-hour");

        String explicit = safeApply(config.getColorExtractor(), slot, null);
        if (explicit != null && explicit.matches(HEX_RE)) {
            content.getStyle().set("--slot-accent", explicit);
            String bg = lighten(explicit, 0.85);
            content.getStyle().set("--slot-bg", bg != null ? bg : explicit);
        } else {
            Object key = config.getCategoryKeyExtractor().apply(slot);
            List<String[]> palette = config.getPalette();
            int idx = key != null ? Math.floorMod(key.hashCode(), palette.size()) : 0;
            String[] colors = palette.get(idx);
            content.getStyle().set("--slot-bg", colors[0]);
            content.getStyle().set("--slot-accent", colors[1]);
        }

        config.getSlotContentPopulator().accept(content, slot);

        if (editable && config.getIsEditable().test(slot)) {
            content.add(buildActionsMenu(slot));
        }

        Tooltip.forComponent(content).setText(buildTooltipText(slot));
        placeInGrid(content, rowIndex + 2, dayIndex + 2, span, 1);

        if (editable && config.getOnEventClick() != null) {
            content.getElement().addEventListener("click", e -> config.getOnEventClick().accept(slot));
        }
        gridContainer.add(content);
    }

    private MenuBar buildActionsMenu(E slot) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE,
                MenuBarVariant.LUMO_ICON, MenuBarVariant.LUMO_SMALL);
        menuBar.addClassName("timetable-slot-actions");
        menuBar.getElement().executeJs("this.addEventListener('click', (e) => e.stopPropagation());");

        MenuItem rootItem = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        SubMenu subMenu = rootItem.getSubMenu();

        config.getActionsMenuBuilder().apply(slot, menuBar);

        if (config.getOnEventEdit() != null) {
            subMenu.addItem(I18n.t("calendar.grid.edit"), e -> config.getOnEventEdit().accept(slot));
        }
        if (config.getOnEventDelete() != null) {
            MenuItem deleteItem = subMenu.addItem(I18n.t("calendar.grid.delete"),
                    e -> config.getOnEventDelete().accept(slot));
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

        if (config.getDateExtractor() != null) {
            LocalDate date = config.getDateExtractor().apply(evt);
            if (date != null) sb.append("\n").append(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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

    private void placeInGrid(Component component, int rowStart, int colStart, int rowSpan, int colSpan) {
        component.getStyle().set("grid-row", rowStart + " / span " + rowSpan);
        component.getStyle().set("grid-column", colStart + " / span " + colSpan);
    }

    private String formatHour(LocalTime time) {
        return time.getHour() + "h";
    }

    private void installNowIndicator() {
        int startMinutes = timeSlots.get(0).getHour() * 60 + timeSlots.get(0).getMinute();
        int stepMinutes = config.getStepMinutes();
        gridContainer.getElement().executeJs(
                "const el = this;" +
                        "if (el._nowInterval) { clearInterval(el._nowInterval); }" +
                        "const update = () => {" +
                        "  const dayCells = Array.from(el.querySelectorAll('.timetable-bg-cell'))" +
                        "      .filter(c => getComputedStyle(c).gridColumnStart === '2');" +
                        "  if (!dayCells.length) return;" +
                        "  let indicator = el.querySelector('.timetable-now-indicator');" +
                        "  const now = new Date();" +
                        "  const totalMin = now.getHours() * 60 + now.getMinutes();" +
                        "  const startMin = " + startMinutes + ";" +
                        "  const endMin = startMin + dayCells.length * " + stepMinutes + ";" +
                        "  const jsDay = now.getDay();" +
                        "  const colIndex = jsDay === 0 ? -1 : jsDay - 1;" +
                        "  if (totalMin < startMin || totalMin > endMin || colIndex < 0) {" +
                        "    if (indicator) indicator.style.display = 'none';" +
                        "    return;" +
                        "  }" +
                        "  const firstCell = dayCells[0];" +
                        "  let totalHeight = 0;" +
                        "  dayCells.forEach(c => totalHeight += c.offsetHeight);" +
                        "  const fraction = (totalMin - startMin) / (endMin - startMin);" +
                        "  const top = firstCell.offsetTop + fraction * totalHeight;" +
                        "  const gridRect = el.getBoundingClientRect();" +
                        "  const firstColRect = firstCell.getBoundingClientRect();" +
                        "  const colWidth = firstColRect.width;" +
                        "  const gutter = firstColRect.left - gridRect.left;" +
                        "  if (!indicator) {" +
                        "    indicator = document.createElement('div');" +
                        "    indicator.className = 'timetable-now-indicator';" +
                        "    indicator.innerHTML = '<span class=\"timetable-now-dot\"></span>';" +
                        "    el.appendChild(indicator);" +
                        "  }" +
                        "  indicator.style.display = 'block';" +
                        "  indicator.style.top = top + 'px';" +
                        "  indicator.style.left = (gutter + colIndex * colWidth) + 'px';" +
                        "  indicator.style.width = colWidth + 'px';" +
                        "};" +
                        "update();" +
                        "el._nowInterval = setInterval(update, 60000);"
        );
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }
}