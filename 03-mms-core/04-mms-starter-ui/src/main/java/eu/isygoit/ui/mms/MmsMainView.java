package eu.isygoit.ui.mms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.RouteScope;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.exception.ServiceOperationException;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.component.DashboardBreakdownPanel;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@RouteAlias(value = "mms/home", layout = MmsMainLayout.class)
@SpringComponent
@RouteScope
@Route(value = "mms", layout = MmsMainLayout.class)
@PermitAll
public class MmsMainView extends ManagementVerticalView {
    private static final int PAGE_SIZE = 500;
    private final SenderConfigService senderConfigService;
    private final MsgTemplateService templateService;
    private final UI ui;
    private final Locale locale;
    private final DashboardBreakdownPanel templateLanguagesPanel;
    private StatCard sendersCard;
    private StatCard templatesCard;
    @Autowired
    public MmsMainView(SenderConfigService senderConfigService, MsgTemplateService templateService) {
        this.senderConfigService = senderConfigService;
        this.templateService = templateService;
        this.ui = UI.getCurrent();
        this.locale = ui.getLocale();
        ui.getPushConfiguration().setPushMode(com.vaadin.flow.shared.communication.PushMode.AUTOMATIC);

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("mms-dashboard");

        add(buildHeader());
        add(buildShortcutsBar());
        add(buildStatsGrid());
        templateLanguagesPanel = new DashboardBreakdownPanel(
                I18n.t("mms.dashboard.templates.by.language"));
        templateLanguagesPanel.setRefreshAction(this::loadStatistics);
        add(templateLanguagesPanel);
        loadStatistics();
    }

    @Override
    public String getPageTitle() {
        return I18n.t("mms.dashboard.title");
    }

    private DashboardShortcutsBar buildShortcutsBar() {
        return new DashboardShortcutsBar(I18n.t("mms.dashboard.quick.actions"), List.of(
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS_CIRCLE, I18n.t("mms.dashboard.quick.create.sender"),
                        () -> UI.getCurrent().navigate("mms/sender-config")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.PLUS_CIRCLE, I18n.t("mms.dashboard.quick.create.template"),
                        () -> UI.getCurrent().navigate("mms/templates")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.START_COG, I18n.t("mms.dashboard.quick.compose.email"),
                        () -> UI.getCurrent().navigate("mms/templates")),
                new DashboardShortcutsBar.Shortcut(VaadinIcon.COG, I18n.t("mms.dashboard.quick.settings"),
                        () -> UI.getCurrent().navigate("mms/sender-config"))
        ));
    }

    private H2 buildHeader() {
        H2 title = new H2(I18n.t("mms.dashboard.title"));
        title.addClassName("wams-dashboard-header");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    /**
     * The two live configuration totals in one {@link StatCardGrid} (4 columns
     * desktop / 3 tablet / 1 mobile). Each card's subtitle (e.g. "Active
     * configurations") is preserved as its tooltip.
     */
    private StatCardGrid buildStatsGrid() {
        sendersCard = new StatCard(VaadinIcon.MAILBOX, StatCard.Variant.PRIMARY,
                I18n.t("mms.dashboard.senders"), null, I18n.t("mms.dashboard.senders.subtitle"))
                .withNavigation(() -> UI.getCurrent().navigate("mms/sender-config"));
        templatesCard = new StatCard(VaadinIcon.FILE_TEXT, StatCard.Variant.PRIMARY,
                I18n.t("mms.dashboard.templates"), null, I18n.t("mms.dashboard.templates.subtitle"))
                .withNavigation(() -> UI.getCurrent().navigate("mms/templates"));

        return new StatCardGrid(sendersCard, templatesCard);
    }

    private void loadStatistics() {
        templateLanguagesPanel.setLoading(true);
        ContextAwareAsync.supplyAsync(this::fetchDashboardData)
                .whenComplete((data, failure) -> ui.access(() -> {
                    if (failure != null) {
                        log.error("Unable to fetch MMS dashboard configuration statistics", failure);
                        templateLanguagesPanel.setError(I18n.t("common.stats.load.error"));
                        ui.push();
                        return;
                    }

                    sendersCard.setValue(String.valueOf(data.senderCount()));
                    templatesCard.setValue(String.valueOf(data.templateCount()));
                    templateLanguagesPanel.setItems(data.languages().entrySet().stream()
                            .map(entry -> new DashboardBreakdownPanel.Item(entry.getKey(), entry.getValue()))
                            .toList());
                    ui.push();
                }));
    }

    private DashboardData fetchDashboardData() {
        PaginatedResponseDto<?> senderBody = senderConfigService.findAll(0, 1).getBody();
        if (senderBody == null) {
            throw new ServiceOperationException("Sender configuration response did not contain data");
        }
        PaginatedResponseDto<MsgTemplateDto> templateBody =
                templateService.findAll(0, PAGE_SIZE).getBody();
        if (templateBody == null) {
            throw new ServiceOperationException("Message template response did not contain data");
        }
        long senderCount = senderBody.getTotalElements();
        long templateCount = templateBody.getTotalElements();
        Map<String, Long> countsByLanguage = new TreeMap<>();
        addLanguageCounts(countsByLanguage, templateBody.getContent());
        long pageCount = (templateCount + PAGE_SIZE - 1) / PAGE_SIZE;
        for (int page = 1; page < pageCount; page++) {
            PaginatedResponseDto<MsgTemplateDto> nextPage =
                    templateService.findAll(page, PAGE_SIZE).getBody();
            if (nextPage == null || nextPage.getContent() == null) {
                throw new ServiceOperationException("Message template page " + page + " was empty");
            }
            addLanguageCounts(countsByLanguage, nextPage.getContent());
        }
        return new DashboardData(senderCount, templateCount, Map.copyOf(countsByLanguage));
    }

    private void addLanguageCounts(Map<String, Long> countsByLanguage, List<MsgTemplateDto> templates) {
        if (templates == null) {
            return;
        }
        templates.stream()
                .map(MsgTemplateDto::getLanguage)
                .filter(language -> language != null)
                .forEach(language -> countsByLanguage.merge(displayLanguage(language), 1L, Long::sum));
    }

    private String displayLanguage(Enum<?> language) {
        String languageTag = language.name().replace('_', '-');
        String displayName = Locale.forLanguageTag(languageTag).getDisplayLanguage(locale);
        return displayName.isBlank() ? language.name() : displayName;
    }

    private record DashboardData(long senderCount, long templateCount, Map<String, Long> languages) {
    }
}
