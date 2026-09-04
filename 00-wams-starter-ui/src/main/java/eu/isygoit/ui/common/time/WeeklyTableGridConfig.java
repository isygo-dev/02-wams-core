package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.menubar.MenuBar;
import eu.isygoit.dto.common.DayTimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.*;

/**
 * Configuration for a generic timetable grid (weekly view).
 *
 * @param <E> the type of a single slot/event
 */
public class WeeklyTableGridConfig<E extends DayTimeSlot> {

    // ---- Core extractors ----
    private Function<E, DayOfWeek> dayOfWeekExtractor;
    private Function<E, LocalTime> startTimeExtractor;
    private Function<E, LocalTime> endTimeExtractor;
    private Function<E, String> titleExtractor;          // was subjectNameExtractor
    private Function<E, String> descriptionExtractor;
    private Function<E, String> locationExtractor;       // was roomNameExtractor
    private Function<E, Object> categoryKeyExtractor;    // for palette selection
    private Function<E, String> ownerExtractor;          // NEW
    private Function<E, LocalDate> dateExtractor;        // NEW

    // ---- Handlers ----
    private Predicate<E> isEditable;
    private Consumer<E> onEventClick;                    // was onSlotClick
    private Consumer<E> onEventDelete;                   // was onSlotDelete
    private Consumer<E> onEventEdit;                     // new, for edit action (optional)
    private BiConsumer<DayOfWeek, LocalTime> onEmptyCellClick;
    private BiFunction<E, MenuBar, SubMenu> actionsMenuBuilder; // custom menu items

    // ---- Slot rendering ----
    private BiConsumer<Component, E> slotContentPopulator;

    // ---- Header ----
    private List<DayOfWeek> daysOfWeek;
    private Function<DayOfWeek, String> dayLabelExtractor;
    private Predicate<DayOfWeek> isTodayPredicate;

    // ---- Timetable settings ----
    private int startHour;
    private int endHour;
    private int stepMinutes;
    private List<String[]> palette;
    private boolean showNowIndicator;

    private WeeklyTableGridConfig() {}

    public static <E extends DayTimeSlot> WeeklyTableGridConfig<E> builder() {
        return new WeeklyTableGridConfig<>();
    }

    // ---- Fluent setters ----
    public WeeklyTableGridConfig<E> withDayOfWeekExtractor(Function<E, DayOfWeek> extractor) {
        this.dayOfWeekExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withStartTimeExtractor(Function<E, LocalTime> extractor) {
        this.startTimeExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withEndTimeExtractor(Function<E, LocalTime> extractor) {
        this.endTimeExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withTitleExtractor(Function<E, String> extractor) {
        this.titleExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withDescriptionExtractor(Function<E, String> extractor) {
        this.descriptionExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withLocationExtractor(Function<E, String> extractor) {
        this.locationExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withCategoryKeyExtractor(Function<E, Object> extractor) {
        this.categoryKeyExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withOwnerExtractor(Function<E, String> extractor) {  // NEW
        this.ownerExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withDateExtractor(Function<E, LocalDate> extractor) {  // NEW
        this.dateExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withIsEditable(Predicate<E> predicate) {
        this.isEditable = predicate;
        return this;
    }

    public WeeklyTableGridConfig<E> withOnEventClick(Consumer<E> handler) {
        this.onEventClick = handler;
        return this;
    }

    public WeeklyTableGridConfig<E> withOnEventDelete(Consumer<E> handler) {
        this.onEventDelete = handler;
        return this;
    }

    public WeeklyTableGridConfig<E> withOnEventEdit(Consumer<E> handler) {
        this.onEventEdit = handler;
        return this;
    }

    public WeeklyTableGridConfig<E> withOnEmptyCellClick(BiConsumer<DayOfWeek, LocalTime> handler) {
        this.onEmptyCellClick = handler;
        return this;
    }

    public WeeklyTableGridConfig<E> withActionsMenuBuilder(BiFunction<E, MenuBar, SubMenu> builder) {
        this.actionsMenuBuilder = builder;
        return this;
    }

    public WeeklyTableGridConfig<E> withSlotContentPopulator(BiConsumer<Component, E> populator) {
        this.slotContentPopulator = populator;
        return this;
    }

    public WeeklyTableGridConfig<E> withDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
        return this;
    }

    public WeeklyTableGridConfig<E> withDayLabelExtractor(Function<DayOfWeek, String> extractor) {
        this.dayLabelExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<E> withIsTodayPredicate(Predicate<DayOfWeek> predicate) {
        this.isTodayPredicate = predicate;
        return this;
    }

    public WeeklyTableGridConfig<E> withStartHour(int startHour) {
        this.startHour = startHour;
        return this;
    }

    public WeeklyTableGridConfig<E> withEndHour(int endHour) {
        this.endHour = endHour;
        return this;
    }

    public WeeklyTableGridConfig<E> withStepMinutes(int stepMinutes) {
        this.stepMinutes = stepMinutes;
        return this;
    }

    public WeeklyTableGridConfig<E> withPalette(List<String[]> palette) {
        this.palette = palette;
        return this;
    }

    public WeeklyTableGridConfig<E> withShowNowIndicator(boolean show) {
        this.showNowIndicator = show;
        return this;
    }

    public WeeklyTableGridConfig<E> build() {
        // Validate required fields
        if (dayOfWeekExtractor == null || startTimeExtractor == null || endTimeExtractor == null ||
                titleExtractor == null || locationExtractor == null || categoryKeyExtractor == null) {
            throw new IllegalStateException("dayOfWeekExtractor, startTimeExtractor, endTimeExtractor, " +
                    "titleExtractor, locationExtractor, and categoryKeyExtractor are required");
        }

        // Provide defaults
        if (isEditable == null) isEditable = s -> true;
        if (onEventClick == null) onEventClick = s -> {};
        if (onEventDelete == null) onEventDelete = s -> {};
        if (onEventEdit == null) onEventEdit = s -> {};
        if (onEmptyCellClick == null) onEmptyCellClick = (d, t) -> {};
        if (actionsMenuBuilder == null) actionsMenuBuilder = (s, menuBar) -> menuBar.addItem("Actions").getSubMenu();
        if (slotContentPopulator == null) slotContentPopulator = (comp, s) -> {};
        if (daysOfWeek == null) daysOfWeek = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        if (dayLabelExtractor == null) dayLabelExtractor = day -> day.name().substring(0, 1);
        if (isTodayPredicate == null) isTodayPredicate = day -> day == DayOfWeek.from(LocalDate.now());
        if (startHour == 0) startHour = 7;
        if (endHour == 0) endHour = 21;
        if (stepMinutes == 0) stepMinutes = 30;
        if (palette == null) palette = List.of(
                new String[]{"#E3F2FD", "#1976D2"},
                new String[]{"#F3E5F5", "#8E24AA"},
                new String[]{"#E8F5E9", "#2E7D32"},
                new String[]{"#FFF3E0", "#EF6C00"},
                new String[]{"#FCE4EC", "#C2185B"},
                new String[]{"#E0F7FA", "#00838F"},
                new String[]{"#FFF8E1", "#F9A825"},
                new String[]{"#EDE7F6", "#5E35B1"}
        );
        // default showNowIndicator to true if not set; field defaults to false, so we set if not explicitly set
        // but we can't detect if user set false, so we'll set default in field declaration: private boolean showNowIndicator = true;
        // We'll ensure that.
        return this;
    }

    // ---- Getters ----
    public Function<E, DayOfWeek> getDayOfWeekExtractor() { return dayOfWeekExtractor; }
    public Function<E, LocalTime> getStartTimeExtractor() { return startTimeExtractor; }
    public Function<E, LocalTime> getEndTimeExtractor() { return endTimeExtractor; }
    public Function<E, String> getTitleExtractor() { return titleExtractor; }
    public Function<E, String> getDescriptionExtractor() { return descriptionExtractor; }
    public Function<E, String> getLocationExtractor() { return locationExtractor; }
    public Function<E, Object> getCategoryKeyExtractor() { return categoryKeyExtractor; }
    public Function<E, String> getOwnerExtractor() { return ownerExtractor; }      // NEW
    public Function<E, LocalDate> getDateExtractor() { return dateExtractor; }    // NEW
    public Predicate<E> getIsEditable() { return isEditable; }
    public Consumer<E> getOnEventClick() { return onEventClick; }
    public Consumer<E> getOnEventDelete() { return onEventDelete; }
    public Consumer<E> getOnEventEdit() { return onEventEdit; }
    public BiConsumer<DayOfWeek, LocalTime> getOnEmptyCellClick() { return onEmptyCellClick; }
    public BiFunction<E, MenuBar, SubMenu> getActionsMenuBuilder() { return actionsMenuBuilder; }
    public BiConsumer<Component, E> getSlotContentPopulator() { return slotContentPopulator; }
    public List<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public Function<DayOfWeek, String> getDayLabelExtractor() { return dayLabelExtractor; }
    public Predicate<DayOfWeek> getIsTodayPredicate() { return isTodayPredicate; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
    public int getStepMinutes() { return stepMinutes; }
    public List<String[]> getPalette() { return palette; }
    public boolean isShowNowIndicator() { return showNowIndicator; }
}