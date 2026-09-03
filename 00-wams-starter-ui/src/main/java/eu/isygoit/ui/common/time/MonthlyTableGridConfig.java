package eu.isygoit.ui.common.time;

import com.vaadin.flow.component.Component;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration for a monthly calendar grid.
 * @param <E> the type of a single event
 */
public class MonthlyTableGridConfig<E> {

    // ---- Data extractors ----
    private final Function<E, LocalDate> dateExtractor;
    private final Function<E, String> titleExtractor;
    private final Function<E, String> colorExtractor;
    private final Function<E, String> descriptionExtractor;

    // ---- Interactions ----
    private final Consumer<LocalDate> onDayClick;          // click on a day cell
    private final Consumer<E> onEventClick;               // click on an event dot
    private final Consumer<E> onEventDelete;              // delete event (via context menu)

    // ---- Rendering ----
    private final BiConsumer<Component, E> eventDotPopulator; // custom dot rendering

    private MonthlyTableGridConfig(Builder<E> builder) {
        this.dateExtractor = builder.dateExtractor;
        this.titleExtractor = builder.titleExtractor;
        this.colorExtractor = builder.colorExtractor;
        this.descriptionExtractor = builder.descriptionExtractor;
        this.onDayClick = builder.onDayClick;
        this.onEventClick = builder.onEventClick;
        this.onEventDelete = builder.onEventDelete;
        this.eventDotPopulator = builder.eventDotPopulator;
    }

    // ---- Getters ----
    public Function<E, LocalDate> getDateExtractor() { return dateExtractor; }
    public Function<E, String> getTitleExtractor() { return titleExtractor; }
    public Function<E, String> getColorExtractor() { return colorExtractor; }
    public Function<E, String> getDescriptionExtractor() { return descriptionExtractor; }
    public Consumer<LocalDate> getOnDayClick() { return onDayClick; }
    public Consumer<E> getOnEventClick() { return onEventClick; }
    public Consumer<E> getOnEventDelete() { return onEventDelete; }
    public BiConsumer<Component, E> getEventDotPopulator() { return eventDotPopulator; }

    // ---- Builder ----
    public static <E> Builder<E> builder() { return new Builder<>(); }

    public static class Builder<E> {
        private Function<E, LocalDate> dateExtractor;
        private Function<E, String> titleExtractor;
        private Function<E, String> colorExtractor;
        private Function<E, String> descriptionExtractor = s -> null;
        private Consumer<LocalDate> onDayClick;
        private Consumer<E> onEventClick;
        private Consumer<E> onEventDelete;
        private BiConsumer<Component, E> eventDotPopulator;

        // Required
        public Builder<E> dateExtractor(Function<E, LocalDate> extractor) {
            this.dateExtractor = extractor; return this;
        }
        public Builder<E> titleExtractor(Function<E, String> extractor) {
            this.titleExtractor = extractor; return this;
        }
        public Builder<E> colorExtractor(Function<E, String> extractor) {
            this.colorExtractor = extractor; return this;
        }

        // Optional
        public Builder<E> descriptionExtractor(Function<E, String> extractor) {
            this.descriptionExtractor = extractor; return this;
        }
        public Builder<E> onDayClick(Consumer<LocalDate> onDayClick) {
            this.onDayClick = onDayClick; return this;
        }
        public Builder<E> onEventClick(Consumer<E> onEventClick) {
            this.onEventClick = onEventClick; return this;
        }
        public Builder<E> onEventDelete(Consumer<E> onEventDelete) {
            this.onEventDelete = onEventDelete; return this;
        }
        public Builder<E> eventDotPopulator(BiConsumer<Component, E> populator) {
            this.eventDotPopulator = populator; return this;
        }

        public MonthlyTableGridConfig<E> build() {
            if (dateExtractor == null || titleExtractor == null || colorExtractor == null) {
                throw new IllegalStateException("dateExtractor, titleExtractor, and colorExtractor are required");
            }
            if (onDayClick == null) onDayClick = d -> {};
            if (onEventClick == null) onEventClick = e -> {};
            if (onEventDelete == null) onEventDelete = e -> {};
            if (eventDotPopulator == null) eventDotPopulator = (comp, e) -> {};
            return new MonthlyTableGridConfig<>(this);
        }
    }
}