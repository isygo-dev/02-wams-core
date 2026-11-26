package eu.isygoit.ui.cms.views.vcalendar;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.dto.data.VCalendarEventDto;
import eu.isygoit.exception.ServiceOperationException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarEventService;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.layout.CmsMainLayout;
import eu.isygoit.ui.cms.views.vcalendar.calendar.CmsCalendarEventSlot;
import eu.isygoit.ui.cms.views.vcalendar.calendar.DailyCalendarView;
import eu.isygoit.ui.cms.views.vcalendar.calendar.MonthlyCalendarView;
import eu.isygoit.ui.cms.views.vcalendar.calendar.WeeklyCalendarView;
import eu.isygoit.ui.common.component.FloatingRefreshButton;
import eu.isygoit.ui.common.component.MaskedDatePicker;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@SpringComponent
@RouteScope
@Route(value = "cms/calendar", layout = CmsMainLayout.class)
@PermitAll
public class CmsCalendarView extends ManagementVerticalView implements AfterNavigationObserver, HasDynamicTitle {

    private static final int PAGE_SIZE = 500;

    private final VCalendarService calendarService;
    private final VCalendarEventService eventService;
    private final UI ui;
    private final AtomicLong loadSequence = new AtomicLong();

    private final ComboBox<VCalendarDto> calendarSelector = new ComboBox<>();
    private final DatePicker datePicker = new MaskedDatePicker();
    private final Button previousButton = new Button(VaadinIcon.CHEVRON_LEFT.create());
    private final Button nextButton = new Button(VaadinIcon.CHEVRON_RIGHT.create());
    private final Button todayButton = new Button(I18n.t("cms.calendar.view.today"));
    private final Span navigationTitle = new Span();
    private final Span navigationSubtitle = new Span();
    private final Span feedback = new Span();
    private final Span eventCount = new Span();
    private final ProgressBar loadingBar = new ProgressBar();
    private final Tabs viewTabs = new Tabs();
    private final Div viewContainer = new Div();    private final FloatingRefreshButton refreshFab = new FloatingRefreshButton(this::refresh);
    private final MonthlyCalendarView monthlyView;
    private final WeeklyCalendarView weeklyView;
    private final DailyCalendarView dailyView;
    private LocalDate currentDate = LocalDate.now();
    private CalendarMode currentMode = CalendarMode.MONTH;
    private List<CmsCalendarEventSlot> events = List.of();

    {
        feedback.addClassName("calendar-feedback");
        eventCount.addClassName("calendar-count");
    }
    @Autowired
    public CmsCalendarView(VCalendarService calendarService, VCalendarEventService eventService) {
        this.calendarService = calendarService;
        this.eventService = eventService;
        this.ui = UI.getCurrent();
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("management-view");
        addClassName("calendar-page");

        monthlyView = new MonthlyCalendarView(this::showDay);
        weeklyView = new WeeklyCalendarView();
        dailyView = new DailyCalendarView();
        viewContainer.setWidthFull();
        viewContainer.addClassName("calendar-view-container");
        viewContainer.addClassName("calendar-grid-panel");
        viewContainer.add(monthlyView, weeklyView, dailyView);

        configureControls();
        add(buildHeader(), buildNavigationBar(), viewTabs, viewContainer, loadingBar, feedback, eventCount, refreshFab);

        datePicker.setValue(currentDate);
        updateVisibleView();
    }

    private void configureControls() {
        calendarSelector.setLabel(I18n.t("cms.calendar.view.calendar.label"));
        calendarSelector.setPlaceholder(I18n.t("cms.calendar.view.calendar.placeholder"));
        calendarSelector.setItemLabelGenerator(calendar ->
                calendar.getName() == null ? calendar.getCode() : calendar.getName());
        calendarSelector.addClassName("calendar-header__selector");
        calendarSelector.setEnabled(false);
        calendarSelector.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                loadEvents(event.getValue());
            }
        });

        Tab monthTab = new Tab(I18n.t("cms.calendar.view.month"));
        Tab weekTab = new Tab(I18n.t("cms.calendar.view.week"));
        Tab dayTab = new Tab(I18n.t("cms.calendar.view.day"));
        viewTabs.add(monthTab, weekTab, dayTab);
        viewTabs.setSelectedIndex(0);
        viewTabs.addSelectedChangeListener(event -> {
            Tab selected = event.getSelectedTab();
            if (selected == monthTab) {
                currentMode = CalendarMode.MONTH;
            } else if (selected == weekTab) {
                currentMode = CalendarMode.WEEK;
            } else if (selected == dayTab) {
                currentMode = CalendarMode.DAY;
            }
            updateVisibleView();
        });

        previousButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        previousButton.addClassName("calendar-nav__arrow");
        previousButton.setTooltipText(I18n.t("cms.calendar.view.previous"));
        previousButton.addClickListener(event -> navigate(-1));

        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClassName("calendar-nav__arrow");
        nextButton.setTooltipText(I18n.t("cms.calendar.view.next"));
        nextButton.addClickListener(event -> navigate(1));

        todayButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        todayButton.addClassName("calendar-nav__today");
        todayButton.addClickListener(event -> {
            currentDate = LocalDate.now();
            datePicker.setValue(currentDate);
            updateVisibleView();
        });

        datePicker.setWidth("180px");
        datePicker.addClassName("calendar-nav__picker");
        datePicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentDate = event.getValue();
                updateVisibleView();
            }
        });
    }

    /**
     * Same header card as the pilot calendar; the calendar selector takes the place of the "add" action.
     */
    private Div buildHeader() {
        Div card = new Div();
        card.addClassName("calendar-header");

        Div iconTile = new Div(VaadinIcon.CALENDAR_O.create());
        iconTile.addClassName("calendar-header__icon");

        H2 title = new H2(I18n.t("cms.calendar.view.schedule.title"));
        title.addClassName("calendar-header__title");

        Span subtitle = new Span(I18n.t("cms.calendar.view.schedule.subtitle"));
        subtitle.addClassName("calendar-header__subtitle");

        VerticalLayout textStack = new VerticalLayout(title, subtitle);
        textStack.setPadding(false);
        textStack.setSpacing(false);
        textStack.addClassName("calendar-header__text");

        HorizontalLayout left = new HorizontalLayout(iconTile, textStack);
        left.setPadding(false);
        left.setSpacing(true);
        left.setAlignItems(Alignment.CENTER);
        left.addClassName("calendar-header__left");

        HorizontalLayout root = new HorizontalLayout(left, calendarSelector);
        root.setWidthFull();
        root.setPadding(false);
        root.setSpacing(true);
        root.setAlignItems(Alignment.CENTER);
        root.setJustifyContentMode(JustifyContentMode.BETWEEN);
        root.addClassName("calendar-header__inner");

        card.add(root);
        return card;
    }

    private HorizontalLayout buildNavigationBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(Alignment.CENTER);
        bar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        bar.addClassName("calendar-nav");

        navigationTitle.addClassName("calendar-nav__title");
        navigationSubtitle.addClassName("calendar-nav__subtitle");

        VerticalLayout centerStack = new VerticalLayout(navigationTitle, navigationSubtitle);
        centerStack.setPadding(false);
        centerStack.setSpacing(false);
        centerStack.setAlignItems(Alignment.CENTER);
        centerStack.addClassName("calendar-nav__center-text");

        HorizontalLayout center = new HorizontalLayout(previousButton, centerStack, nextButton);
        center.setPadding(false);
        center.setSpacing(true);
        center.setAlignItems(Alignment.CENTER);
        center.addClassName("calendar-nav__center");

        HorizontalLayout actions = new HorizontalLayout(datePicker, todayButton);
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.setAlignItems(Alignment.CENTER);
        actions.addClassName("calendar-nav__actions");

        bar.add(center, actions);
        bar.expand(center);
        return bar;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String calendarId = event.getLocation().getQueryParameters()
                .getSingleParameter("calendarId").orElse(null);
        Long requestedCalendarId = null;
        if (calendarId != null) {
            try {
                requestedCalendarId = Long.parseLong(calendarId);
            } catch (NumberFormatException exception) {
                log.warn("Ignoring invalid CMS calendar id in route: {}", calendarId);
                showError(I18n.t("cms.calendar.view.calendar.not.found"));
                return;
            }
        }
        loadCalendars(requestedCalendarId);
    }

    private void loadCalendars(Long preferredCalendarId) {
        clearFeedback();
        setLoading(true);
        long sequence = loadSequence.incrementAndGet();
        ContextAwareAsync.supplyAsync(this::fetchCalendars)
                .whenComplete((calendars, failure) -> ui.access(() -> {
                    if (sequence != loadSequence.get()) {
                        return;
                    }
                    setLoading(false);
                    if (failure != null) {
                        log.error("Failed to load calendars for calendar view", failure);
                        showError(I18n.t("cms.calendar.view.load.error", failure.getMessage()));
                        ui.push();
                        return;
                    }

                    calendarSelector.setItems(calendars);
                    calendarSelector.clear();
                    calendarSelector.setEnabled(!calendars.isEmpty());
                    if (calendars.isEmpty()) {
                        events = List.of();
                        updateVisibleView();
                        showError(I18n.t("cms.calendar.view.no.calendars"));
                        ui.push();
                        return;
                    }

                    VCalendarDto selected = preferredCalendarId == null
                            ? calendars.get(0)
                            : calendars.stream()
                            .filter(calendar -> preferredCalendarId.equals(calendar.getId()))
                            .findFirst()
                            .orElse(null);
                    if (selected == null) {
                        events = List.of();
                        updateVisibleView();
                        showError(I18n.t("cms.calendar.view.calendar.not.found"));
                    } else {
                        clearFeedback();
                        calendarSelector.setValue(selected);
                    }
                    ui.push();
                }));
    }

    private List<VCalendarDto> fetchCalendars() {
        PaginatedResponseDto<VCalendarDto> firstPage = calendarService.findAll(0, PAGE_SIZE).getBody();
        if (firstPage == null) {
            throw new ServiceOperationException("Calendar response did not contain data");
        }
        List<VCalendarDto> calendars = new ArrayList<>(
                firstPage.getContent() == null ? List.of() : firstPage.getContent());
        long pageCount = (firstPage.getTotalElements() + PAGE_SIZE - 1) / PAGE_SIZE;
        for (int page = 1; page < pageCount; page++) {
            PaginatedResponseDto<VCalendarDto> response = calendarService.findAll(page, PAGE_SIZE).getBody();
            if (response == null || response.getContent() == null) {
                throw new ServiceOperationException("Calendar page " + page + " was empty");
            }
            calendars.addAll(response.getContent());
        }
        return calendars;
    }

    private void loadEvents(VCalendarDto calendar) {
        setLoading(true);
        clearFeedback();
        long sequence = loadSequence.incrementAndGet();
        ContextAwareAsync.supplyAsync(() -> fetchEvents(calendar))
                .whenComplete((loadedEvents, failure) -> ui.access(() -> {
                    if (sequence != loadSequence.get()) {
                        return;
                    }
                    setLoading(false);
                    if (failure != null) {
                        log.error("Failed to load events for calendar {}", calendar.getName(), failure);
                        events = List.of();
                        updateVisibleView();
                        showError(I18n.t("cms.calendar.view.events.load.error", failure.getMessage()));
                    } else {
                        events = loadedEvents;
                        updateVisibleView();
                        eventCount.setText(I18n.t("cms.calendar.view.events.count", events.size()));
                    }
                    ui.push();
                }));
    }

    private List<CmsCalendarEventSlot> fetchEvents(VCalendarDto calendar) {
        ResponseEntity<List<VCalendarEventDto>> response =
                eventService.getAllByTenantAndCalendarName(calendar.getTenant(), calendar.getName());
        List<VCalendarEventDto> content = response == null || response.getBody() == null
                ? List.of() : response.getBody();
        return content.stream()
                .filter(event -> event.getStartDate() != null)
                .map(CmsCalendarEventSlot::from)
                .toList();
    }

    private void navigate(int direction) {
        currentDate = switch (currentMode) {
            case MONTH -> currentDate.plusMonths(direction);
            case WEEK -> currentDate.plusWeeks(direction);
            case DAY -> currentDate.plusDays(direction);
        };
        datePicker.setValue(currentDate);
        updateVisibleView();
    }

    private void showDay(LocalDate date) {
        currentDate = date;
        datePicker.setValue(date);
        viewTabs.setSelectedIndex(2);
        currentMode = CalendarMode.DAY;
        updateVisibleView();
    }

    private void updateVisibleView() {
        LocalDate weekStart = currentDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        navigationTitle.setText(currentMode == CalendarMode.MONTH
                ? DateTimeFormatter.ofPattern("MMMM yyyy", I18n.getCurrentLocale()).format(currentDate)
                : currentMode == CalendarMode.WEEK
                ? DateTimeFormatter.ofPattern("dd MMM yyyy", I18n.getCurrentLocale()).format(weekStart)
                + " – " + DateTimeFormatter.ofPattern("dd MMM yyyy", I18n.getCurrentLocale()).format(weekEnd)
                : DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", I18n.getCurrentLocale()).format(currentDate));
        navigationSubtitle.setText(currentMode == CalendarMode.MONTH
                ? I18n.t("cms.calendar.view.month.subtitle")
                : currentMode == CalendarMode.WEEK
                ? I18n.t("cms.calendar.view.week.subtitle")
                : I18n.t("cms.calendar.view.day.subtitle"));

        monthlyView.setEvents(events);
        weeklyView.setEvents(events);
        dailyView.setEvents(events);
        monthlyView.setVisible(currentMode == CalendarMode.MONTH);
        weeklyView.setVisible(currentMode == CalendarMode.WEEK);
        dailyView.setVisible(currentMode == CalendarMode.DAY);

        switch (currentMode) {
            case MONTH -> monthlyView.setMonth(YearMonth.from(currentDate));
            case WEEK -> weeklyView.setWeek(currentDate);
            case DAY -> dailyView.setDay(currentDate);
        }
    }

    private void setLoading(boolean loading) {
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(loading);
        loadingBar.setWidthFull();
        calendarSelector.setEnabled(!loading);
        refreshFab.setLoading(loading);
    }

    private void refresh() {
        VCalendarDto selected = calendarSelector.getValue();
        if (selected == null) {
            loadCalendars(null);
        } else {
            loadEvents(selected);
        }
    }

    private void showError(String message) {
        feedback.setText(message);
        feedback.addClassName("calendar-feedback--error");
    }

    private void clearFeedback() {
        feedback.setText("");
        feedback.removeClassName("calendar-feedback--error");
        eventCount.setText("");
    }

    @Override
    public String getPageTitle() {
        return I18n.t("cms.calendar.view.schedule.title");
    }

    private enum CalendarMode {
        MONTH, WEEK, DAY
    }




}
