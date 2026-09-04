package eu.isygoit.ui.common.time;

import java.time.LocalTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Configuration for {@link DailyTableGrid}.
 * Uses the same pattern as WeeklyTableGridConfig.
 */
public class DailyTableGridConfig<E> {

    // ---- Core extractors ----
    private Function<E, LocalTime> startTimeExtractor;
    private Function<E, LocalTime> endTimeExtractor;
    private Function<E, String> titleExtractor;
    private Function<E, String> descriptionExtractor;
    private Function<E, String> colorExtractor;          // fallback (not used if palette is used)
    private Function<E, String> locationExtractor;
    private Function<E, Object> categoryKeyExtractor;    // for palette selection

    // ---- Handlers ----
    private Consumer<E> onEventClick;
    private Consumer<E> onEventDelete;
    private Consumer<E> onEventEdit;
    private BiConsumer<VerticalLayout, E> slotContentPopulator;
    private Predicate<E> isEditable = e -> true;

    // ---- Timetable settings (same as weekly) ----
    private int startHour = 7;
    private int endHour = 21;
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

    // ---- Empty cell click (creates new event) ----
    private BiConsumer<LocalTime, LocalTime> onEmptyCellClick; // (startTime, endTime)

    private DailyTableGridConfig() {}

    public static <E> DailyTableGridConfig<E> builder() { return new DailyTableGridConfig<>(); }

    // ---- Fluent setters ----
    public DailyTableGridConfig<E> withStartTimeExtractor(Function<E, LocalTime> extractor) {
        this.startTimeExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withEndTimeExtractor(Function<E, LocalTime> extractor) {
        this.endTimeExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withTitleExtractor(Function<E, String> extractor) {
        this.titleExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withDescriptionExtractor(Function<E, String> extractor) {
        this.descriptionExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withColorExtractor(Function<E, String> extractor) {
        this.colorExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withLocationExtractor(Function<E, String> extractor) {
        this.locationExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withCategoryKeyExtractor(Function<E, Object> extractor) {
        this.categoryKeyExtractor = extractor; return this;
    }
    public DailyTableGridConfig<E> withOnEventClick(Consumer<E> handler) {
        this.onEventClick = handler; return this;
    }
    public DailyTableGridConfig<E> withOnEventDelete(Consumer<E> handler) {
        this.onEventDelete = handler; return this;
    }
    public DailyTableGridConfig<E> withOnEventEdit(Consumer<E> handler) {
        this.onEventEdit = handler; return this;
    }
    public DailyTableGridConfig<E> withSlotContentPopulator(BiConsumer<VerticalLayout, E> populator) {
        this.slotContentPopulator = populator; return this;
    }
    public DailyTableGridConfig<E> withIsEditable(Predicate<E> predicate) {
        this.isEditable = predicate; return this;
    }
    public DailyTableGridConfig<E> withStartHour(int startHour) {
        this.startHour = startHour; return this;
    }
    public DailyTableGridConfig<E> withEndHour(int endHour) {
        this.endHour = endHour; return this;
    }
    public DailyTableGridConfig<E> withStepMinutes(int stepMinutes) {
        this.stepMinutes = stepMinutes; return this;
    }
    public DailyTableGridConfig<E> withPalette(List<String[]> palette) {
        this.palette = palette; return this;
    }
    public DailyTableGridConfig<E> withShowNowIndicator(boolean show) {
        this.showNowIndicator = show; return this;
    }
    public DailyTableGridConfig<E> withOnEmptyCellClick(BiConsumer<LocalTime, LocalTime> handler) {
        this.onEmptyCellClick = handler; return this;
    }

    public DailyTableGridConfig<E> build() {
        // Validate required fields
        if (startTimeExtractor == null || endTimeExtractor == null || titleExtractor == null) {
            throw new IllegalStateException("startTimeExtractor, endTimeExtractor, and titleExtractor are required");
        }
        return this;
    }

    // ---- Getters ----
    public Function<E, LocalTime> getStartTimeExtractor() { return startTimeExtractor; }
    public Function<E, LocalTime> getEndTimeExtractor() { return endTimeExtractor; }
    public Function<E, String> getTitleExtractor() { return titleExtractor; }
    public Function<E, String> getDescriptionExtractor() { return descriptionExtractor; }
    public Function<E, String> getColorExtractor() { return colorExtractor; }
    public Function<E, String> getLocationExtractor() { return locationExtractor; }
    public Function<E, Object> getCategoryKeyExtractor() { return categoryKeyExtractor; }
    public Consumer<E> getOnEventClick() { return onEventClick; }
    public Consumer<E> getOnEventDelete() { return onEventDelete; }
    public Consumer<E> getOnEventEdit() { return onEventEdit; }
    public BiConsumer<VerticalLayout, E> getSlotContentPopulator() { return slotContentPopulator; }
    public Predicate<E> getIsEditable() { return isEditable; }
    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
    public int getStepMinutes() { return stepMinutes; }
    public List<String[]> getPalette() { return palette; }
    public boolean isShowNowIndicator() { return showNowIndicator; }
    public BiConsumer<LocalTime, LocalTime> getOnEmptyCellClick() { return onEmptyCellClick; }
}