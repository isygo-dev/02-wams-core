package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.theme.lumo.LumoUtility;import eu.isygoit.dto.common.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * A reusable, generic timetable grid that renders slots in a weekly view.
 * All behaviour and rendering is controlled via {@link TimeTableGridConfig}.
 *
 * @param <S> the type of a single slot
 */
public class TimeTableGrid<S extends TimeSlot> extends VerticalLayout {

    private final TimeTableGridConfig<S> config;
    private final Div gridContainer = new Div();
    private final List<LocalTime> timeSlots = new ArrayList<>();

    private List<S> slots = Collections.emptyList();
    private boolean editable = true;

    public TimeTableGrid(TimeTableGridConfig<S> config) {
        this.config = config;
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("timetable-grid-wrapper");

        // Generate time slots
        int startHour = config.getStartHour();
        int endHour = config.getEndHour();
        int step = config.getStepMinutes();
        for (int h = startHour; h < endHour; h++) {
            for (int m = 0; m < 60; m += step) {
                timeSlots.add(LocalTime.of(h, m));
            }
        }
        // add the end marker (e.g. 20:00) as the last time slot (not bookable)
        timeSlots.add(LocalTime.of(endHour, 0));

        gridContainer.setWidthFull();
        gridContainer.addClassName("timetable-grid");
        add(gridContainer);
    }

    /**
     * Sets the slots to display and whether the timetable is editable.
     */
    public void setItems(List<S> slots, boolean editable) {
        this.slots = slots != null ? slots : Collections.emptyList();
        this.editable = editable;
        buildGrid();
    }

    private void buildGrid() {
        gridContainer.removeAll();
        if (slots.isEmpty()) {
            Div empty = new Div();
            empty.setText("No slots to display"); // override via i18n if needed
            empty.addClassName(LumoUtility.TextColor.SECONDARY);
            empty.addClassName(LumoUtility.Padding.LARGE);
            gridContainer.add(empty);
            return;
        }

        List<DayOfWeek> days = config.getDaysOfWeek();
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        // Bookable time slots: all except the last marker
        List<LocalTime> bookableStarts = timeSlots.subList(0, timeSlots.size() - 1);
        LocalTime endMarker = timeSlots.get(timeSlots.size() - 1);

        Map<LocalTime, Integer> rowIndexByTime = new HashMap<>();
        for (int i = 0; i < bookableStarts.size(); i++) {
            rowIndexByTime.put(bookableStarts.get(i), i);
        }

        // Map slots to their starting row and calculate spans
        Map<DayOfWeek, Map<Integer, S>> slotsByDayRow = new HashMap<>();
        Set<String> coveredRows = new HashSet<>();
        for (S slot : slots) {
            LocalTime start = config.getStartTimeExtractor().apply(slot);
            Integer rowIndex = rowIndexByTime.get(start);
            if (rowIndex == null) continue;
            DayOfWeek day = config.getDayOfWeekExtractor().apply(slot);
            slotsByDayRow.computeIfAbsent(day, d -> new HashMap<>()).put(rowIndex, slot);
            int span = spanFor(slot);
            for (int r = rowIndex; r < rowIndex + span; r++) {
                coveredRows.add(day.name() + "-" + r);
            }
        }

        // Build row template
        StringBuilder rowTemplate = new StringBuilder("auto ");
        for (LocalTime start : bookableStarts) {
            rowTemplate.append(start.getMinute() == 0 ? "var(--tt-row-full) " : "var(--tt-row-half) ");
        }
        rowTemplate.append("var(--tt-row-end)");
        gridContainer.getStyle().set("grid-template-columns", "64px repeat(" + days.size() + ", 1fr)");
        gridContainer.getStyle().set("grid-template-rows", rowTemplate.toString().trim());

        // Header
        buildHeader(today);

        // Time labels and cells
        for (int rowIndex = 0; rowIndex < bookableStarts.size(); rowIndex++) {
            LocalTime start = bookableStarts.get(rowIndex);
            buildTimeLabel(start, rowIndex, false);

            for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
                DayOfWeek day = days.get(dayIndex);
                boolean isStart = slotsByDayRow.containsKey(day) && slotsByDayRow.get(day).containsKey(rowIndex);
                boolean isCovered = coveredRows.contains(day.name() + "-" + rowIndex);

                buildBackgroundCell(day, dayIndex, rowIndex, start, today, isStart || isCovered);

                if (isStart) {
                    S slot = slotsByDayRow.get(day).get(rowIndex);
                    buildSlotChip(slot, dayIndex, rowIndex, spanFor(slot));
                }
            }
        }

        buildTimeLabel(endMarker, bookableStarts.size(), true);

        if (config.isShowNowIndicator()) {
            installNowIndicator();
        }
    }

    private int spanFor(S slot) {
        LocalTime start = config.getStartTimeExtractor().apply(slot);
        LocalTime end = config.getEndTimeExtractor().apply(slot);
        int durationMinutes = (int) java.time.Duration.between(start, end).toMinutes();
        return Math.max(1, (int) Math.round(durationMinutes / (double) config.getStepMinutes()));
    }

    private void buildHeader(DayOfWeek today) {
        Div corner = new Div();
        corner.addClassName("timetable-grid-header");
        placeInGrid(corner, 1, 1, 1, 1);
        gridContainer.add(corner);

        List<DayOfWeek> days = config.getDaysOfWeek();
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            DayOfWeek day = days.get(dayIndex);
            String label = config.getDayLabelExtractor().apply(day);
            Span dayLabel = new Span(label);
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
        if (isClosingLabel) {
            timeLabel.addClassName("timetable-time-label--end");
        }
        timeLabel.addClassName(LumoUtility.FontSize.XSMALL);
        timeLabel.addClassName(LumoUtility.TextColor.SECONDARY);
        placeInGrid(timeLabel, rowIndex + 2, 1, 1, 1);
        gridContainer.add(timeLabel);
    }

    private void buildBackgroundCell(DayOfWeek day, int dayIndex, int rowIndex, LocalTime start,
                                     DayOfWeek today, boolean occupied) {
        Div cell = new Div();
        cell.addClassName("timetable-bg-cell");
        cell.addClassName(start.getMinute() == 0 ? "hour-full" : "hour-half");
        if (day == today) {
            cell.addClassName("timetable-grid-col-today");
        }
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

    private void buildSlotChip(S slot, int dayIndex, int rowIndex, int span) {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.addClassName("timetable-slot-content");
        content.addClassName("timetable-slot-chip");
        if (span > 1) {
            content.addClassName("timetable-grid-cell--multi-hour");
        }

        // Apply category colours
        Object key = config.getCategoryKeyExtractor().apply(slot);
        List<String[]> palette = config.getPalette();
        int idx = key != null ? Math.floorMod(key.hashCode(), palette.size()) : 0;
        String[] colors = palette.get(idx);
        content.getStyle().set("--slot-bg", colors[0]);
        content.getStyle().set("--slot-accent", colors[1]);

        // Populate content via config (default or custom)
        config.getSlotContentPopulator().accept(content, slot);

        // Add actions menu if editable
        if (editable && config.getIsEditable().test(slot)) {
            content.add(buildActionsMenu(slot));
        }

        // Tooltip
        String tooltipText = buildTooltipText(slot);
        Tooltip.forComponent(content).setText(tooltipText);

        placeInGrid(content, rowIndex + 2, dayIndex + 2, span, 1);

        // Click on slot
        if (editable && config.getOnSlotClick() != null) {
            content.getElement().addEventListener("click", e -> config.getOnSlotClick().accept(slot));
        }

        gridContainer.add(content);
    }

    private MenuBar buildActionsMenu(S slot) {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE,
                MenuBarVariant.LUMO_ICON,
                MenuBarVariant.LUMO_SMALL);
        menuBar.addClassName("timetable-slot-actions");
        menuBar.getElement().executeJs("this.addEventListener('click', (e) => e.stopPropagation());");

        MenuItem rootItem = menuBar.addItem(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        SubMenu subMenu = rootItem.getSubMenu();

        // Let config add custom items
        config.getActionsMenuBuilder().apply(slot, menuBar);

        // Default: add edit and delete if config provides handlers
        if (config.getOnSlotClick() != null) {
            subMenu.addItem("Edit", e -> config.getOnSlotClick().accept(slot));
        }
        if (config.getOnSlotDelete() != null) {
            MenuItem deleteItem = subMenu.addItem("Delete", e -> config.getOnSlotDelete().accept(slot));
            deleteItem.getStyle().set("color", "var(--lumo-error-text-color)");
        }

        return menuBar;
    }

    private String buildTooltipText(S slot) {
        StringBuilder sb = new StringBuilder();
        String subject = config.getSubjectNameExtractor().apply(slot);
        String teacher = config.getTeacherNameExtractor().apply(slot);
        String room = config.getRoomNameExtractor().apply(slot);
        LocalTime start = config.getStartTimeExtractor().apply(slot);
        LocalTime end = config.getEndTimeExtractor().apply(slot);
        String desc = config.getDescriptionExtractor().apply(slot);

        sb.append(subject)
                .append(" • ").append(teacher)
                .append(" • ").append(room)
                .append(" • ").append(start).append("–").append(end);
        if (desc != null && !desc.isBlank()) {
            sb.append(" — ").append(desc);
        }
        return sb.toString();
    }

    private void placeInGrid(Component component, int rowStart, int colStart, int rowSpan, int colSpan) {
        component.getElement().getStyle().set("grid-row", rowStart + (rowSpan > 1 ? " / span " + rowSpan : ""));
        component.getElement().getStyle().set("grid-column", colStart + (colSpan > 1 ? " / span " + colSpan : ""));
    }

    private String formatHour(LocalTime time) {
        return time.getHour() + "h";
    }

    private void installNowIndicator() {
        int startMinutes = timeSlots.get(0).getHour() * 60 + timeSlots.get(0).getMinute();

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
                        "  const endMin = startMin + dayCells.length * 30;" +
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
}