package eu.isygoit.ui.plan.views.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import eu.isygoit.dto.common.CriteriaFieldDto;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.PlanJobMinDto;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.plan.PlanFeignService;
import eu.isygoit.ui.common.filters.FilterBar;
import eu.isygoit.ui.common.view.AbstractPaginatedView;
import eu.isygoit.ui.plan.common.PlanEnumTag;
import eu.isygoit.ui.plan.common.PlanErrors;
import eu.isygoit.ui.plan.common.PlanFormats;
import eu.isygoit.ui.plan.common.PlanPush;
import eu.isygoit.ui.plan.layout.PlanMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Planning jobs list (route {@code plan/jobs}), aligned on the EDYRA pilot {@code StudentView}: server-side
 * pagination and criteria filtering through {@link AbstractPaginatedView}, a card / table switch, a shared
 * {@link FilterBar} and the floating refresh button.
 *
 * <p>The filter bar is metadata driven: the criteria served by {@code getAnnotatedCriteria()} on the job
 * ({@code code}, {@code clientRef}, {@code engine}, {@code status}) are rendered as text boxes, the status
 * ({@link IEnumJobStatusCode.Types}) is resolved through a per-field enum hint. Table columns sort the loaded
 * page only (the backend has no sort parameter). The row actions are written once in {@link PlanJobActions} and
 * reached from {@link PlanJobCard}, whose buttons the table reuses.</p>
 */
@Slf4j
@SpringComponent
@RouteScope
@Route(value = "plan/jobs", layout = PlanMainLayout.class)
@PermitAll
public class PlanJobsView extends AbstractPaginatedView<PlanJobMinDto> {

    private final PlanFeignService service;
    private final PlanJobActions actions;

    @Autowired
    public PlanJobsView(PlanFeignService service, ObjectMapper objectMapper) {
        this.service = service;
        this.actions = new PlanJobActions(service, objectMapper);
        PlanPush.ensure(UI.getCurrent());

        initPaginatedView();   // template method
    }

    // ══════════════════════════════════════════════════════════════
    //  Contract: fetch / render / filter / create
    // ══════════════════════════════════════════════════════════════

    @Override
    protected PaginatedResponseDto<PlanJobMinDto> fetchPage(int page, int size) {
        var response = service.findAll(page, size);
        return response != null ? response.getBody() : null;
    }

    /**
     * The backend's {@code /filter} endpoint returns the full DTO, hence the widened return type.
     */
    @Override
    protected PaginatedResponseDto<? extends PlanJobMinDto> fetchFilteredPage(String criteria, int page, int size) {
        var response = service.findAllFilteredByCriteria(criteria, page, size);
        return response != null ? response.getBody() : null;
    }

    /**
     * Criteria metadata (field name to field type) of the filter bar, in the order the backend declares it.
     */
    @Override
    protected Map<String, String> fetchCriteriaMetadata() {
        try {
            var response = service.getAnnotatedCriteria();
            if (response != null && response.getBody() != null) {
                LinkedHashMap<String, String> ordered = new LinkedHashMap<>(response.getBody().size());
                for (CriteriaFieldDto field : response.getBody()) {
                    ordered.put(field.name(), field.type());
                }
                return ordered;
            }
        } catch (Exception e) {
            log.warn("Cannot load criteria metadata", e);
        }
        return new LinkedHashMap<>();
    }

    @Override
    protected Component renderItem(PlanJobMinDto item) {
        return new PlanJobCard(this, service, item);
    }

    /**
     * Table mode: the same page as the cards, one row per job; the actions column reuses the card buttons.
     */
    @Override
    protected boolean configureGrid(Grid<PlanJobMinDto> grid) {
        grid.addColumn(PlanJobMinDto::getCode).setHeader(I18n.t("plan.ui.jobs.code"))
                .setAutoWidth(true).setFlexGrow(0).setSortable(true);
        grid.addColumn(PlanJobMinDto::getClientRef).setHeader(I18n.t("plan.ui.jobs.client.ref"))
                .setAutoWidth(true).setFlexGrow(1).setSortable(true);
        grid.addColumn(PlanJobMinDto::getEngine).setHeader(I18n.t("plan.ui.jobs.engine"))
                .setAutoWidth(true).setSortable(true);
        grid.addComponentColumn(job -> job.getStatus() == null ? new Span() : PlanEnumTag.ofStatus(job.getStatus()))
                .setHeader(I18n.t("plan.ui.jobs.status")).setAutoWidth(true).setFlexGrow(0)
                .setComparator(Comparator.comparing(PlanJobMinDto::getStatus,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        grid.addColumn(job -> PlanFormats.dateTime(job.getSubmittedAt())).setHeader(I18n.t("plan.ui.jobs.submitted"))
                .setAutoWidth(true).setComparator(byInstant(PlanJobMinDto::getSubmittedAt));
        grid.addColumn(job -> PlanFormats.dateTime(job.getStartedAt())).setHeader(I18n.t("plan.ui.jobs.started"))
                .setAutoWidth(true).setComparator(byInstant(PlanJobMinDto::getStartedAt));
        grid.addColumn(job -> PlanFormats.dateTime(job.getCompletedAt())).setHeader(I18n.t("plan.ui.jobs.completed"))
                .setAutoWidth(true).setComparator(byInstant(PlanJobMinDto::getCompletedAt));
        grid.addColumn(job -> PlanFormats.duration(job.getStartedAt(), job.getCompletedAt()))
                .setHeader(I18n.t("plan.ui.jobs.duration")).setAutoWidth(true);
        grid.addColumn(PlanJobMinDto::getMessage).setHeader(I18n.t("plan.ui.jobs.message")).setFlexGrow(2);
        addCardActionsColumn(grid);
        return true;
    }

    private static Comparator<PlanJobMinDto> byInstant(java.util.function.Function<PlanJobMinDto, java.time.Instant> instant) {
        return Comparator.comparing(instant, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    protected boolean matchesSearch(PlanJobMinDto job, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String lower = search.toLowerCase(Locale.ROOT);
        return contains(job.getCode(), lower)
                || contains(job.getClientRef(), lower)
                || contains(job.getEngine(), lower)
                || contains(job.getMessage(), lower)
                || contains(job.getErrorCode(), lower)
                || contains(String.valueOf(job.getId()), lower);
    }

    private boolean contains(String value, String lower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lower);
    }

    @Override
    protected void openCreateDialog() {
        actions.openSubmit(this::refresh);
    }

    @Override
    protected String extractErrorMessage(Exception e) {
        return PlanErrors.describe(e);
    }

    // ══════════════════════════════════════════════════════════════
    //  Optional hooks
    // ══════════════════════════════════════════════════════════════

    @Override
    protected String[] getRootCssClasses() {
        return new String[]{"plan-jobs-view"};
    }

    /**
     * Metadata-driven filter bar; the status criterion is an enum, resolved through a per-field hint.
     */
    @Override
    protected Component buildAdditionalToolbar() {
        Map<String, String> criteria = fetchCriteriaMetadata();

        FilterBar bar = new FilterBar(
                criteria,
                java.util.List.of(),                           // no simple-name registry
                Map.of(                                        // per-field enum hints
                        "status", IEnumJobStatusCode.Types.class
                ));
        bar.setOnApply(this::applyCriteria);
        bar.setOnClear(this::clearCriteria);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════
    //  i18n keys
    // ══════════════════════════════════════════════════════════════

    @Override
    protected String getTitleKey() {
        return "plan.ui.jobs.title";
    }

    @Override
    protected String getSubtitleKey() {
        return "plan.ui.jobs.subtitle";
    }

    @Override
    protected String getCreateButtonKey() {
        return "plan.ui.jobs.create.button";
    }

    @Override
    protected String getSearchPlaceholderKey() {
        return "plan.ui.jobs.search";
    }

    @Override
    protected String getEmptyTitleKey() {
        return "plan.ui.jobs.empty.title";
    }

    @Override
    protected String getEmptyDescriptionKey() {
        return "plan.ui.jobs.empty.description";
    }

    @Override
    protected VaadinIcon getEmptyIcon() {
        return VaadinIcon.TASKS;
    }

    @Override
    protected String getLoadErrorKey() {
        return "plan.ui.jobs.load.error";
    }

    // ══════════════════════════════════════════════════════════════
    //  Page title
    // ══════════════════════════════════════════════════════════════

    @Override
    public String getPageTitle() {
        return I18n.t("plan.ui.jobs.title");
    }

    // ══════════════════════════════════════════════════════════════
    //  Card action helpers (called by PlanJobCard)
    // ══════════════════════════════════════════════════════════════

    public void openDetailsDialog(PlanJobMinDto job) {
        actions.openDetails(job, this::refresh);
    }

    public void openEditDialog(PlanJobMinDto job) {
        actions.openEdit(job, this::refresh);
    }

    public void cloneJob(PlanJobMinDto job) {
        actions.openClone(job, this::refresh);
    }

    public void openLogs(PlanJobMinDto job) {
        actions.openLogs(job.getId());
    }

    public void stopJob(PlanJobMinDto job) {
        actions.confirmStop(job, this::refresh, this::showLoading);
    }

    public void cancelJob(PlanJobMinDto job) {
        actions.confirmCancel(job, this::refresh, this::showLoading);
    }

    public void deleteJob(PlanJobMinDto job) {
        actions.confirmDelete(job, this::refresh, this::showLoading);
    }

    public void exportResult(PlanJobMinDto job) {
        actions.exportResult(job);
    }
}
