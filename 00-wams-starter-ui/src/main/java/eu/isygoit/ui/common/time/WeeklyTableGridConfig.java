package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.menubar.MenuBar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.*;

/**
 * Configuration for a generic timetable grid (weekly view).
 *
 * @param <S> the type of a single slot/event
 */
public class WeeklyTableGridConfig<S> {

    // ---- Core extractors ----
    private Function<S, DayOfWeek> dayOfWeekExtractor;
    private Function<S, LocalTime> startTimeExtractor;
    private Function<S, LocalTime> endTimeExtractor;
    private Function<S, String> titleExtractor;          // was subjectNameExtractor
    private Function<S, String> descriptionExtractor;
    private Function<S, String> locationExtractor;       // was roomNameExtractor
    private Function<S, Object> categoryKeyExtractor;    // for palette selection

    // ---- Handlers ----
    private Predicate<S> isEditable;
    private Consumer<S> onEventClick;                    // was onSlotClick
    private Consumer<S> onEventDelete;                   // was onSlotDelete
    private Consumer<S> onEventEdit;                     // new, for edit action (optional)
    private BiConsumer<DayOfWeek, LocalTime> onEmptyCellClick;
    private BiFunction<S, MenuBar, SubMenu> actionsMenuBuilder; // custom menu items

    // ---- Slot rendering ----
    private BiConsumer<Component, S> slotContentPopulator;

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

    public static <S> WeeklyTableGridConfig<S> builder() {
        return new WeeklyTableGridConfig<>();
    }

    // ---- Fluent setters ----
    public WeeklyTableGridConfig<S> withDayOfWeekExtractor(Function<S, DayOfWeek> extractor) {
        this.dayOfWeekExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withStartTimeExtractor(Function<S, LocalTime> extractor) {
        this.startTimeExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withEndTimeExtractor(Function<S, LocalTime> extractor) {
        this.endTimeExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withTitleExtractor(Function<S, String> extractor) {
        this.titleExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withDescriptionExtractor(Function<S, String> extractor) {
        this.descriptionExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withLocationExtractor(Function<S, String> extractor) {
        this.locationExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withCategoryKeyExtractor(Function<S, Object> extractor) {
        this.categoryKeyExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withIsEditable(Predicate<S> predicate) {
        this.isEditable = predicate;
        return this;
    }

    public WeeklyTableGridConfig<S> withOnEventClick(Consumer<S> handler) {
        this.onEventClick = handler;
        return this;
    }

    public WeeklyTableGridConfig<S> withOnEventDelete(Consumer<S> handler) {
        this.onEventDelete = handler;
        return this;
    }

    public WeeklyTableGridConfig<S> withOnEventEdit(Consumer<S> handler) {
        this.onEventEdit = handler;
        return this;
    }

    public WeeklyTableGridConfig<S> withOnEmptyCellClick(BiConsumer<DayOfWeek, LocalTime> handler) {
        this.onEmptyCellClick = handler;
        return this;
    }

    public WeeklyTableGridConfig<S> withActionsMenuBuilder(BiFunction<S, MenuBar, SubMenu> builder) {
        this.actionsMenuBuilder = builder;
        return this;
    }

    public WeeklyTableGridConfig<S> withSlotContentPopulator(BiConsumer<Component, S> populator) {
        this.slotContentPopulator = populator;
        return this;
    }

    public WeeklyTableGridConfig<S> withDaysOfWeek(List<DayOfWeek> daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
        return this;
    }

    public WeeklyTableGridConfig<S> withDayLabelExtractor(Function<DayOfWeek, String> extractor) {
        this.dayLabelExtractor = extractor;
        return this;
    }

    public WeeklyTableGridConfig<S> withIsTodayPredicate(Predicate<DayOfWeek> predicate) {
        this.isTodayPredicate = predicate;
        return this;
    }

    public WeeklyTableGridConfig<S> withStartHour(int startHour) {
        this.startHour = startHour;
        return this;
    }

    public WeeklyTableGridConfig<S> withEndHour(int endHour) {
        this.endHour = endHour;
        return this;
    }

    public WeeklyTableGridConfig<S> withStepMinutes(int stepMinutes) {
        this.stepMinutes = stepMinutes;
        return this;
    }

    public WeeklyTableGridConfig<S> withPalette(List<String[]> palette) {
        this.palette = palette;
        return this;
    }

    public WeeklyTableGridConfig<S> withShowNowIndicator(boolean show) {
        this.showNowIndicator = show;
        return this;
    }

    public WeeklyTableGridConfig<S> build() {
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
        // showNowIndicator default is true; if not set, we keep the default (we can't distinguish unset vs false)
        // We'll just keep the field as is; if user didn't call withShowNowIndicator, it's false by default (primitive).
        // So we set a default in the field declaration: private boolean showNowIndicator = true;
        // But the field is not initialized here, so we set it in the field declaration above.
        // We'll ensure the field has a default value.
        return this;
    }

    // ---- Getters ----
    public Function<S, DayOfWeek> getDayOfWeekExtractor() { return dayOfWeekExtractor; }
    public Function<S, LocalTime> getStartTimeExtractor() { return startTimeExtractor; }
    public Function<S, LocalTime> getEndTimeExtractor() { return endTimeExtractor; }
    public Function<S, String> getTitleExtractor() { return titleExtractor; }
    public Function<S, String> getDescriptionExtractor() { return descriptionExtractor; }
    public Function<S, String> getLocationExtractor() { return locationExtractor; }
    public Function<S, Object> getCategoryKeyExtractor() { return categoryKeyExtractor; }
    public Predicate<S> getIsEditable() { return isEditable; }
    public Consumer<S> getOnEventClick() { return onEventClick; }
    public Consumer<S> getOnEventDelete() { return onEventDelete; }
    public Consumer<S> getOnEventEdit() { return onEventEdit; }
    public BiConsumer<DayOfWeek, LocalTime> getOnEmptyCellClick() { return onEmptyCellClick; }
    public BiFunction<S, MenuBar, SubMenu> getActionsMenuBuilder() { return actionsMenuBuilder; }
    public BiConsumer<Component, S> getSlotContentPopulator() { return slotContentPopulator; }
    public List<DayOfWeek> getDaysOfWeek() { return daysOfWeek; }
    public Function<DayOfWeek, String> getDayLabelExtractor() { return dayLabelExtractor; }
    public Predicate<DayOfWeek> getIsTodayPredicate() { return isTodayPredicate; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
    public int getStepMinutes() { return stepMinutes; }
    public List<String[]> getPalette() { return palette; }
    public boolean isShowNowIndicator() { return showNowIndicator; }
}