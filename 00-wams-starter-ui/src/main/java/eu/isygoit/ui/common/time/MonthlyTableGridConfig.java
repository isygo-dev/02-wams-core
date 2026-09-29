package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import eu.isygoit.dto.common.DayTimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration for a monthly calendar grid.
 *
 * @param <E> the type of a single event
 */
public class MonthlyTableGridConfig<E extends DayTimeSlot> {

    // ---- Core extractors ----
    private Function<E, LocalDate> dateExtractor;
    private Function<E, String> titleExtractor;
    private Function<E, String> colorExtractor;
    private Function<E, String> descriptionExtractor;
    private Function<E, String> ownerExtractor;
    private Function<E, String> locationExtractor;
    private Function<E, LocalTime> startTimeExtractor;
    private Function<E, LocalTime> endTimeExtractor;

    // ---- Handlers ----
    private Consumer<LocalDate> onDayClick;
    private Consumer<E> onEventClick;
    private Consumer<E> onEventDelete;
    private Consumer<E> onEventEdit;

    // ---- Rendering ----
    private BiConsumer<Component, E> eventDotPopulator;

    private MonthlyTableGridConfig() {
    }

    public static <E extends DayTimeSlot> MonthlyTableGridConfig<E> builder() {
        return new MonthlyTableGridConfig<>();
    }

    // ---- Fluent setters ----
    public MonthlyTableGridConfig<E> withDateExtractor(Function<E, LocalDate> extractor) {
        this.dateExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withTitleExtractor(Function<E, String> extractor) {
        this.titleExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withColorExtractor(Function<E, String> extractor) {
        this.colorExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withDescriptionExtractor(Function<E, String> extractor) {
        this.descriptionExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withOwnerExtractor(Function<E, String> extractor) {
        this.ownerExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withLocationExtractor(Function<E, String> extractor) {
        this.locationExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withStartTimeExtractor(Function<E, LocalTime> extractor) {
        this.startTimeExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withEndTimeExtractor(Function<E, LocalTime> extractor) {
        this.endTimeExtractor = extractor;
        return this;
    }

    public MonthlyTableGridConfig<E> withOnDayClick(Consumer<LocalDate> handler) {
        this.onDayClick = handler;
        return this;
    }

    public MonthlyTableGridConfig<E> withOnEventClick(Consumer<E> handler) {
        this.onEventClick = handler;
        return this;
    }

    public MonthlyTableGridConfig<E> withOnEventDelete(Consumer<E> handler) {
        this.onEventDelete = handler;
        return this;
    }

    public MonthlyTableGridConfig<E> withOnEventEdit(Consumer<E> handler) {
        this.onEventEdit = handler;
        return this;
    }

    public MonthlyTableGridConfig<E> withEventDotPopulator(BiConsumer<Component, E> populator) {
        this.eventDotPopulator = populator;
        return this;
    }

    public MonthlyTableGridConfig<E> build() {
        // Required
        if (dateExtractor == null || titleExtractor == null || colorExtractor == null) {
            throw new IllegalStateException(
                    "dateExtractor, titleExtractor, and colorExtractor are required");
        }

        // Safe defaults for every optional extractor / handler.
        if (descriptionExtractor == null) descriptionExtractor = e -> "";
        if (ownerExtractor == null)       ownerExtractor       = e -> "";
        if (locationExtractor == null)    locationExtractor    = e -> "";
        if (startTimeExtractor == null)   startTimeExtractor   = e -> null;
        if (endTimeExtractor == null)     endTimeExtractor     = e -> null;

        if (onDayClick == null)          onDayClick          = d -> {};
        if (onEventClick == null)        onEventClick        = e -> {};
        if (onEventDelete == null)       onEventDelete       = e -> {};
        if (onEventEdit == null)         onEventEdit         = e -> {};
        if (eventDotPopulator == null)   eventDotPopulator   = (comp, e) -> {};

        return this;
    }

    // ---- Getters ----
    public Function<E, LocalDate> getDateExtractor()        { return dateExtractor; }
    public Function<E, String>    getTitleExtractor()       { return titleExtractor; }
    public Function<E, String>    getDescriptionExtractor() { return descriptionExtractor; }
    public Function<E, String>    getColorExtractor()       { return colorExtractor; }
    public Function<E, String>    getOwnerExtractor()       { return ownerExtractor; }
    public Function<E, String>    getLocationExtractor()    { return locationExtractor; }
    public Function<E, LocalTime> getStartTimeExtractor()   { return startTimeExtractor; }
    public Function<E, LocalTime> getEndTimeExtractor()     { return endTimeExtractor; }

    public Consumer<LocalDate> getOnDayClick()    { return onDayClick; }
    public Consumer<E> getOnEventClick()          { return onEventClick; }
    public Consumer<E> getOnEventDelete()         { return onEventDelete; }
    public Consumer<E> getOnEventEdit()           { return onEventEdit; }
    public BiConsumer<Component, E> getEventDotPopulator() { return eventDotPopulator; }
}