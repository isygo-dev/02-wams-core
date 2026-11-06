package eu.isygoit.ui.cms.views.vcalendar;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.VCalendarDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.cms.VCalendarService;
import eu.isygoit.ui.cms.layout.CmsMainLayout;
import eu.isygoit.ui.cms.views.vcalendar.dialog.CreateVCalendarDialog;
import eu.isygoit.ui.cms.views.vcalendar.dialog.UpdateVCalendarDialog;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringComponent
@RouteScope
@Route(value = "cms/calendars", layout = CmsMainLayout.class)
@PermitAll
public class VCalendarManagementView extends AbstractPaginatedView<VCalendarDto> {

    private final VCalendarService calendarService;
    private final ComboBox<Integer> pageSizeSelect = new ComboBox<>();
    private final com.vaadin.flow.component.html.Span totalCountLabel =
            new com.vaadin.flow.component.html.Span();
    private final TextField tenantFilter = new TextField();
    private String currentTenant = "";

    @Autowired
    public VCalendarManagementView(VCalendarService calendarService) {
        this.calendarService = calendarService;
        pageSize = 10;
        initPaginatedView();
    }

    @Override
    protected PaginatedResponseDto<VCalendarDto> fetchPage(int page, int size) {
        var response = calendarService.findAll(page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected PaginatedResponseDto<? extends VCalendarDto> fetchFilteredPage(String criteria, int page, int size) {
        var response = calendarService.findAllFilteredByCriteria(criteria, page, size);
        return response != null ? response.getBody() : null;
    }

    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        var response = calendarService.getAnnotatedCriteria();
        return response != null && response.getBody() != null
                ? response.getBody().stream().collect(Collectors.toMap(
                field -> field.name(), field -> field.type(), (first, second) -> second,
                java.util.LinkedHashMap::new))
                : Map.of();
    }

    @Override
    protected boolean configureGrid(Grid<VCalendarDto> grid) {
        grid.addColumn(VCalendarDto::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(VCalendarDto::getCode).setHeader("Code").setAutoWidth(true);
        grid.addColumn(VCalendarDto::getTenant).setHeader("Tenant").setFlexGrow(1);
        grid.addColumn(VCalendarDto::getLocked).setHeader("Locked").setAutoWidth(true);
        addCardActionsColumn(grid);
        return true;
    }

    @Override
    protected Component renderItem(VCalendarDto item) {
        return new VCalendarCard(this, calendarService, item, this::loadPageZero);
    }

    @Override
    protected boolean matchesSearch(VCalendarDto calendar, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String query = search.toLowerCase();
        return (calendar.getName() != null && calendar.getName().toLowerCase().contains(query))
                || (calendar.getCode() != null && calendar.getCode().toLowerCase().contains(query))
                || (calendar.getDescription() != null && calendar.getDescription().toLowerCase().contains(query));
    }

    @Override
    protected boolean matchesAdditionalFilters(VCalendarDto calendar) {
        return currentTenant == null || currentTenant.isBlank()
                || currentTenant.equals(calendar.getTenant());
    }

    @Override
    protected void openCreateDialog() {
        new CreateVCalendarDialog(this, calendarService, this::loadPageZero).open();
    }

    @Override
    protected void onSearchChanged() {
        loadPageZero();
    }

    @Override
    protected List<Component> buildToolbarFilters() {
        tenantFilter.setPlaceholder(I18n.t("cms.calendar.view.tenant.placeholder"));
        tenantFilter.setClearButtonVisible(true);
        tenantFilter.setWidth("180px");
        tenantFilter.addValueChangeListener(event -> {
            currentTenant = event.getValue();
            loadPageZero();
        });
        return List.of(tenantFilter);
    }

    @Override
    protected List<Component> buildToolbarActions() {
        pageSizeSelect.setItems(5, 10, 20, 50);
        pageSizeSelect.setValue(pageSize);
        pageSizeSelect.setWidth("100px");
        pageSizeSelect.addValueChangeListener(event -> {
            if (event.getValue() != null && event.getValue() != pageSize) {
                pageSize = event.getValue();
                loadPageZero();
            }
        });
        totalCountLabel.addClassNames("management-total-count", "pagination-total-count");
        return List.of(pageSizeSelect, totalCountLabel);
    }

    @Override
    protected void onPageLoaded() {
        totalCountLabel.setText(I18n.t("cms.calendar.view.total.count", totalElements));
    }

    @Override
    protected String extractErrorMessage(Exception exception) {
        if (exception instanceof FeignException feignException
                && feignException.contentUTF8() != null && !feignException.contentUTF8().isBlank()) {
            return feignException.contentUTF8();
        }
        return exception.getMessage();
    }

    @Override
    protected String getTitleKey() {
        return "cms.calendar.view.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "cms.calendar.view.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "cms.calendar.view.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "cms.calendar.view.search.placeholder";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "cms.calendar.view.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "cms.calendar.view.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.CALENDAR_O;
    }

    @Override
    protected String getLoadErrorKey() {
        return "cms.calendar.view.load.error";
    }

    @Override
    public String getPageTitle() {
        return I18n.t(getTitleKey());
    }

    public void loadPageZero() {
        loadPage(0);
    }

    public void openUpdateCalendarDialog(VCalendarDto calendar, Runnable onSuccess) {
        new UpdateVCalendarDialog(this, calendarService, calendar, onSuccess).open();
    }
}
