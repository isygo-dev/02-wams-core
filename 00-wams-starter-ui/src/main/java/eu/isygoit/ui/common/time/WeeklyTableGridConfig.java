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
 * Configuration for a generic timetable grid.
 *
 * @param <S> the type of a single slot
 */
public class WeeklyTableGridConfig<S> {

    // ---- Data extractors ----
    private final Function<S, DayOfWeek> dayOfWeekExtractor;
    private final Function<S, LocalTime> startTimeExtractor;
    private final Function<S, LocalTime> endTimeExtractor;
    private final Function<S, String> subjectNameExtractor;
    private final Function<S, String> teacherNameExtractor;
    private final Function<S, String> roomNameExtractor;
    private final Function<S, String> descriptionExtractor;
    private final Function<S, Object> categoryKeyExtractor;

    // ---- Editable and actions ----
    private final Predicate<S> isEditable;
    private final Consumer<S> onSlotClick;              // e.g. open update dialog
    private final Consumer<S> onSlotDelete;             // e.g. open delete dialog
    private final BiConsumer<DayOfWeek, LocalTime> onEmptyCellClick; // e.g. open create dialog
    private final BiFunction<S, MenuBar, SubMenu> actionsMenuBuilder; // add custom menu items

    // ---- Slot rendering ----
    private final BiConsumer<Component, S> slotContentPopulator; // fills the slot chip

    // ---- Header ----
    private final List<DayOfWeek> daysOfWeek;
    private final Function<DayOfWeek, String> dayLabelExtractor;
    private final Predicate<DayOfWeek> isTodayPredicate;

    // ---- Time range ----
    private final int startHour;
    private final int endHour;        // exclusive, i.e. first time beyond the last displayed slot
    private final int stepMinutes;

    // ---- Appearance ----
    private final List<String[]> palette; // list of [background, accent] color pairs
    private final boolean showNowIndicator;

    // ---- Builder ----
    private WeeklyTableGridConfig(Builder<S> builder) {
        this.dayOfWeekExtractor = builder.dayOfWeekExtractor;
        this.startTimeExtractor = builder.startTimeExtractor;
        this.endTimeExtractor = builder.endTimeExtractor;
        this.subjectNameExtractor = builder.subjectNameExtractor;
        this.teacherNameExtractor = builder.teacherNameExtractor;
        this.roomNameExtractor = builder.roomNameExtractor;
        this.descriptionExtractor = builder.descriptionExtractor;
        this.categoryKeyExtractor = builder.categoryKeyExtractor;
        this.isEditable = builder.isEditable;
        this.onSlotClick = builder.onSlotClick;
        this.onSlotDelete = builder.onSlotDelete;
        this.onEmptyCellClick = builder.onEmptyCellClick;
        this.actionsMenuBuilder = builder.actionsMenuBuilder;
        this.slotContentPopulator = builder.slotContentPopulator;
        this.daysOfWeek = builder.daysOfWeek;
        this.dayLabelExtractor = builder.dayLabelExtractor;
        this.isTodayPredicate = builder.isTodayPredicate;
        this.startHour = builder.startHour;
        this.endHour = builder.endHour;
        this.stepMinutes = builder.stepMinutes;
        this.palette = builder.palette;
        this.showNowIndicator = builder.showNowIndicator;
    }

    // ---- Getters ----
    public Function<S, DayOfWeek> getDayOfWeekExtractor() { return dayOfWeekExtractor; }
    public Function<S, LocalTime> getStartTimeExtractor() { return startTimeExtractor; }
    public Function<S, LocalTime> getEndTimeExtractor() { return endTimeExtractor; }
    public Function<S, String> getSubjectNameExtractor() { return subjectNameExtractor; }
    public Function<S, String> getTeacherNameExtractor() { return teacherNameExtractor; }
    public Function<S, String> getRoomNameExtractor() { return roomNameExtractor; }
    public Function<S, String> getDescriptionExtractor() { return descriptionExtractor; }
    public Function<S, Object> getCategoryKeyExtractor() { return categoryKeyExtractor; }
    public Predicate<S> getIsEditable() { return isEditable; }
    public Consumer<S> getOnSlotClick() { return onSlotClick; }
    public Consumer<S> getOnSlotDelete() { return onSlotDelete; }
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

    // ---- Builder ----
    public static <S> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S> {
        private Function<S, DayOfWeek> dayOfWeekExtractor;
        private Function<S, LocalTime> startTimeExtractor;
        private Function<S, LocalTime> endTimeExtractor;
        private Function<S, String> subjectNameExtractor;
        private Function<S, String> teacherNameExtractor;
        private Function<S, String> roomNameExtractor;
        private Function<S, String> descriptionExtractor = s -> null;
        private Function<S, Object> categoryKeyExtractor;

        private Predicate<S> isEditable = s -> true;
        private Consumer<S> onSlotClick;
        private Consumer<S> onSlotDelete;
        private BiConsumer<DayOfWeek, LocalTime> onEmptyCellClick;
        private BiFunction<S, MenuBar, SubMenu> actionsMenuBuilder;

        private BiConsumer<Component, S> slotContentPopulator;

        private List<DayOfWeek> daysOfWeek = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        );
        private Function<DayOfWeek, String> dayLabelExtractor = day -> day.name().substring(0, 1);
        private Predicate<DayOfWeek> isTodayPredicate = day -> day == DayOfWeek.from(LocalDate.now());

        private int startHour = 7;
        private int endHour = 20;       // 20:00 is the last time label, no slots start at 20:00
        private int stepMinutes = 30;

        private List<String[]> palette = List.of(
                new String[]{"#E3F2FD", "#1976D2"},
                new String[]{"#F3E5F5", "#8E24AA"},
                new String[]{"#E8F5E9", "#2E7D32"},
                new String[]{"#FFF3E0", "#EF6C00"},
                new String[]{"#FCE4EC", "#C2185B"},
                new String[]{"#E0F7FA", "#00838F"},
                new String[]{"#FFF8E1", "#F9A825"},
                new String[]{"#EDE7F6", "#5E35B1"}
        );
        private boolean showNowIndicator = true;

        // Required fields
        public Builder<S> dayOfWeekExtractor(Function<S, DayOfWeek> extractor) {
            this.dayOfWeekExtractor = extractor;
            return this;
        }
        public Builder<S> startTimeExtractor(Function<S, LocalTime> extractor) {
            this.startTimeExtractor = extractor;
            return this;
        }
        public Builder<S> endTimeExtractor(Function<S, LocalTime> extractor) {
            this.endTimeExtractor = extractor;
            return this;
        }
        public Builder<S> subjectNameExtractor(Function<S, String> extractor) {
            this.subjectNameExtractor = extractor;
            return this;
        }
        public Builder<S> teacherNameExtractor(Function<S, String> extractor) {
            this.teacherNameExtractor = extractor;
            return this;
        }
        public Builder<S> roomNameExtractor(Function<S, String> extractor) {
            this.roomNameExtractor = extractor;
            return this;
        }
        public Builder<S> categoryKeyExtractor(Function<S, Object> extractor) {
            this.categoryKeyExtractor = extractor;
            return this;
        }

        // Optional
        public Builder<S> descriptionExtractor(Function<S, String> extractor) {
            this.descriptionExtractor = extractor;
            return this;
        }
        public Builder<S> isEditable(Predicate<S> isEditable) {
            this.isEditable = isEditable;
            return this;
        }
        public Builder<S> onSlotClick(Consumer<S> onSlotClick) {
            this.onSlotClick = onSlotClick;
            return this;
        }
        public Builder<S> onSlotDelete(Consumer<S> onSlotDelete) {
            this.onSlotDelete = onSlotDelete;
            return this;
        }
        public Builder<S> onEmptyCellClick(BiConsumer<DayOfWeek, LocalTime> onEmptyCellClick) {
            this.onEmptyCellClick = onEmptyCellClick;
            return this;
        }
        public Builder<S> actionsMenuBuilder(BiFunction<S, MenuBar, SubMenu> actionsMenuBuilder) {
            this.actionsMenuBuilder = actionsMenuBuilder;
            return this;
        }
        public Builder<S> slotContentPopulator(BiConsumer<Component, S> populator) {
            this.slotContentPopulator = populator;
            return this;
        }
        public Builder<S> daysOfWeek(List<DayOfWeek> daysOfWeek) {
            this.daysOfWeek = daysOfWeek;
            return this;
        }
        public Builder<S> dayLabelExtractor(Function<DayOfWeek, String> extractor) {
            this.dayLabelExtractor = extractor;
            return this;
        }
        public Builder<S> isTodayPredicate(Predicate<DayOfWeek> predicate) {
            this.isTodayPredicate = predicate;
            return this;
        }
        public Builder<S> startHour(int startHour) {
            this.startHour = startHour;
            return this;
        }
        public Builder<S> endHour(int endHour) {
            this.endHour = endHour;
            return this;
        }
        public Builder<S> stepMinutes(int stepMinutes) {
            this.stepMinutes = stepMinutes;
            return this;
        }
        public Builder<S> palette(List<String[]> palette) {
            this.palette = palette;
            return this;
        }
        public Builder<S> showNowIndicator(boolean show) {
            this.showNowIndicator = show;
            return this;
        }

        public WeeklyTableGridConfig<S> build() {
            // Validate required fields
            if (dayOfWeekExtractor == null || startTimeExtractor == null || endTimeExtractor == null ||
                    subjectNameExtractor == null || teacherNameExtractor == null || roomNameExtractor == null ||
                    categoryKeyExtractor == null) {
                throw new IllegalStateException("All extractors must be provided");
            }
            // Provide defaults for actions if not set
            if (onSlotClick == null) onSlotClick = s -> {};
            if (onSlotDelete == null) onSlotDelete = s -> {};
            if (onEmptyCellClick == null) onEmptyCellClick = (d, t) -> {};
            if (actionsMenuBuilder == null) {
                actionsMenuBuilder = (s, menuBar) -> {
                    SubMenu sub = menuBar.addItem("Actions").getSubMenu();
                    // default: no items; subclasses can override
                    return sub;
                };
            }
            if (slotContentPopulator == null) {
                slotContentPopulator = (component, slot) -> {
                    // default: do nothing
                };
            }
            return new WeeklyTableGridConfig<>(this);
        }
    }
}