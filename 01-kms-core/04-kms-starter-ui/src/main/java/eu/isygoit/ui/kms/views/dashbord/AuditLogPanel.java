package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.KmsDtos.AuditLogResponse;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.remote.kms.KmsApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AuditLogPanel.class);
    private static final int PAGE_SIZE = 20;

    private final KmsApiService kmsApiService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private ComboBox<KeyUsageStatsPanel.KeyOption> keyCombo;
    private DatePicker fromDatePicker;
    private DatePicker toDatePicker;
    private Button loadButton;
    private RowCardList<AuditLogResponse.LogEntry> list;
    private HorizontalLayout paginationBar;
    private Button prevButton;
    private Button nextButton;
    private Span pageInfoSpan;
    private List<AuditLogResponse.LogEntry> allLogs = new ArrayList<>();
    private int currentPage = 0;

    public AuditLogPanel(KmsApiService kmsApiService, UI ui) {
        this.kmsApiService = kmsApiService;
        this.ui = ui;
        buildUI();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(true);
        addClassName("kms-parta-panel-spaced");

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 title = new H3(I18n.t("kms.audit.log.title"));
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(title, loadingBar);
        add(titleRow);

        HorizontalLayout filterBar = new HorizontalLayout();
        filterBar.setWidthFull();
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);
        filterBar.addClassName("kms-parta-filter-bar-wrap");
        filterBar.addClassName("audit-filter-bar");

        keyCombo = new ComboBox<>(I18n.t("kms.audit.log.kms.key"));
        keyCombo.setPlaceholder(I18n.t("kms.audit.log.select.key"));
        keyCombo.setItemLabelGenerator(KeyUsageStatsPanel.KeyOption::getDisplayName);
        keyCombo.setWidth("300px");
        keyCombo.setRequired(true);

        fromDatePicker = new DatePicker(I18n.t("kms.audit.log.from.date"));
        fromDatePicker.setPlaceholder(I18n.t("kms.audit.log.date.placeholder"));
        fromDatePicker.setWidth("180px");

        toDatePicker = new DatePicker(I18n.t("kms.audit.log.to.date"));
        toDatePicker.setPlaceholder(I18n.t("kms.audit.log.date.placeholder"));
        toDatePicker.setWidth("180px");

        loadButton = new Button(I18n.t("kms.audit.log.load.button"), VaadinIcon.SEARCH.create());
        loadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loadButton.addClickListener(e -> loadLogs());

        filterBar.add(keyCombo, fromDatePicker, toDatePicker, loadButton);
        add(filterBar);

        list = new RowCardList<>();
        list.setWidthFull();
        list.setVisible(false);
        list.cardFactory(entry -> {
            LocalDateTime ts = entry.getTimestamp();
            Long execTime = entry.getExecutionTimeMs();
            return RowCard.create()
                    .title(ts != null ? DateHelper.formatToHumanReadable(ts) : "-")
                    .tag(KmsEnumTag.ofOrUnknown(entry.getAction(), null))
                    .tag(KmsEnumTag.ofValue(entry.getStatus(), null))
                    .fact(I18n.t("kms.audit.log.column.key.id"), entry.getKeyId())
                    .fact(I18n.t("kms.audit.log.column.principal"), entry.getPrincipal())
                    .fact(I18n.t("kms.audit.log.column.ip.address"), entry.getIpAddress())
                    .fact(I18n.t("kms.audit.log.column.exec.time"), execTime != null ? String.valueOf(execTime) : null)
                    .fact(I18n.t("kms.audit.log.column.error.message"), entry.getErrorMessage());
        });
        add(list);

        paginationBar = new HorizontalLayout();
        paginationBar.setWidthFull();
        paginationBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        paginationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        paginationBar.setSpacing(true);
        paginationBar.setVisible(false);
        prevButton = new Button(I18n.t("kms.audit.log.pagination.previous"), VaadinIcon.ANGLE_LEFT.create());
        prevButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        prevButton.addClickListener(e -> changePage(-1));
        nextButton = new Button(I18n.t("kms.audit.log.pagination.next"), VaadinIcon.ANGLE_RIGHT.create());
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton.addClickListener(e -> changePage(1));
        pageInfoSpan = new Span();
        paginationBar.add(prevButton, pageInfoSpan, nextButton);
        add(paginationBar);
    }

    public void setKeyOptions(List<KeyUsageStatsPanel.KeyOption> options) {
        keyCombo.setItems(options);
    }

    private void loadLogs() {
        KeyUsageStatsPanel.KeyOption selected = keyCombo.getValue();
        if (selected == null) {
            Notification.show(I18n.t("kms.audit.log.select.key.warning"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        ui.access(() -> {
            loadButton.setEnabled(false);
            list.setVisible(false);
            paginationBar.setVisible(false);
            loadingBar.setVisible(true);

            try {
                LocalDateTime from = fromDatePicker.getValue() != null ? fromDatePicker.getValue().atStartOfDay() : null;
                LocalDateTime to = toDatePicker.getValue() != null ? toDatePicker.getValue().atTime(LocalTime.MAX) : null;

                ResponseEntity<AuditLogResponse> response = kmsApiService.getAuditLogs(selected.getKeyId(), from, to, 500);
                List<AuditLogResponse.LogEntry> logs = (response.getBody() != null && response.getBody().getLogs() != null)
                        ? response.getBody().getLogs()
                        : new ArrayList<>();

                allLogs = logs;
                currentPage = 0;
                updateList();
                list.setVisible(true);
                paginationBar.setVisible(!logs.isEmpty());
                loadingBar.setVisible(false);
                loadButton.setEnabled(true);

                String msg = logs.isEmpty() ? I18n.t("kms.audit.log.no.logs") : I18n.t("kms.audit.log.loaded", logs.size());
                Notification.show(msg, 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(logs.isEmpty() ? NotificationVariant.LUMO_WARNING : NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                log.error("Failed to load audit logs", ex);
                Notification.show(I18n.t("kms.audit.log.load.error"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                loadingBar.setVisible(false);
                loadButton.setEnabled(true);
            }
        });
    }

    private void updateList() {
        if (allLogs.isEmpty()) {
            list.setItems(new ArrayList<>());
            pageInfoSpan.setText(I18n.t("kms.audit.log.pagination.no.logs"));
            prevButton.setEnabled(false);
            nextButton.setEnabled(false);
            return;
        }
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allLogs.size());
        list.setItems(allLogs.subList(start, end));
        int totalPages = (int) Math.ceil((double) allLogs.size() / PAGE_SIZE);
        pageInfoSpan.setText(I18n.t("kms.audit.log.pagination.page", currentPage + 1, totalPages));
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalPages - 1);
    }

    private void changePage(int delta) {
        int totalPages = (int) Math.ceil((double) allLogs.size() / PAGE_SIZE);
        int newPage = currentPage + delta;
        if (newPage >= 0 && newPage < totalPages) {
            currentPage = newPage;
            updateList();
        }
    }
}