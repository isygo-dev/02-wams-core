package eu.isygoit.ui.mms;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import eu.isygoit.remote.mms.SenderConfigService;
import eu.isygoit.ui.common.component.DashboardShortcutsBar;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import eu.isygoit.ui.common.view.ManagementVerticalView;
import eu.isygoit.ui.mms.layout.MmsMainLayout;
import eu.isygoit.ui.mms.views.dashboard.EmailStatisticsPanel;
import eu.isygoit.ui.mms.views.dashboard.SenderConfigPanel;
import eu.isygoit.ui.mms.views.dashboard.TemplateStatisticsPanel;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@RouteAlias(value = "mms/home", layout = MmsMainLayout.class)
@UIScope
@Route(value = "mms", layout = MmsMainLayout.class)
@PageTitle("MMS Dashboard")
@PermitAll
public class MmsMainView extends ManagementVerticalView {

    private final SenderConfigService senderConfigService;
    private final MsgTemplateService templateService;

    private final EmailStatisticsPanel emailStatsPanel;
    private final TemplateStatisticsPanel templateStatsPanel;
    private final SenderConfigPanel senderConfigPanel;

    @Autowired
    public MmsMainView(SenderConfigService senderConfigService, MsgTemplateService templateService) {
        this.senderConfigService = senderConfigService;
        this.templateService = templateService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("mms-dashboard");

        add(buildShortcutsBar());
        add(buildHeader());
        add(buildStatsGrid());
        emailStatsPanel = new EmailStatisticsPanel();
        add(emailStatsPanel);
        templateStatsPanel = new TemplateStatisticsPanel();
        add(templateStatsPanel);
        senderConfigPanel = new SenderConfigPanel();
        add(senderConfigPanel);
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
        title.addClassName("wams-dashboard-title");
        title.addClassName(LumoUtility.FontSize.XXLARGE);
        return title;
    }

    /**
     * The 5 overview stats, unified into one {@link StatCardGrid} (4 columns
     * desktop / 3 tablet / 1 mobile). Each card's subtitle (e.g. "Active
     * configurations") is preserved as its tooltip.
     */
    private StatCardGrid buildStatsGrid() {
        StatCard sendersCard = new StatCard(VaadinIcon.MAILBOX, StatCard.Variant.PRIMARY,
                I18n.t("mms.dashboard.senders"), fetchSenderCount(), I18n.t("mms.dashboard.senders.subtitle"))
                .withNavigation(() -> UI.getCurrent().navigate("mms/sender-config"));
        StatCard templatesCard = new StatCard(VaadinIcon.FILE_TEXT, StatCard.Variant.PRIMARY,
                I18n.t("mms.dashboard.templates"), fetchTemplateCount(), I18n.t("mms.dashboard.templates.subtitle"))
                .withNavigation(() -> UI.getCurrent().navigate("mms/templates"));
        // "Emails Sent" / "Queued": this app doesn't actually send/track emails
        // yet, so these remain illustrative placeholder figures.
        StatCard emailsSentCard = new StatCard(VaadinIcon.ENVELOPE, StatCard.Variant.PRIMARY,
                I18n.t("mms.dashboard.emails.sent"), "1,234", I18n.t("mms.dashboard.emails.sent.subtitle"));
        StatCard queuedCard = new StatCard(VaadinIcon.INFO_CIRCLE, StatCard.Variant.WARNING,
                I18n.t("mms.dashboard.queued"), "12", I18n.t("mms.dashboard.queued.subtitle"));
        StatCard deliveryRateCard = new StatCard(VaadinIcon.TRENDING_UP, StatCard.Variant.SUCCESS,
                I18n.t("mms.dashboard.delivery.rate"), "98.4%", I18n.t("mms.dashboard.delivery.rate.subtitle"));

        return new StatCardGrid(sendersCard, templatesCard, emailsSentCard, queuedCard, deliveryRateCard);
    }

    private String fetchSenderCount() {
        try {
            PaginatedResponseDto<?> body = senderConfigService.findAll(0, 1).getBody();
            return body != null ? String.valueOf(body.getTotalElements()) : "0";
        } catch (Exception e) {
            log.warn("Unable to fetch sender configuration count for MMS dashboard: {}", e.getMessage());
            return I18n.t("mms.common.value.notAvailable");
        }
    }

    private String fetchTemplateCount() {
        try {
            PaginatedResponseDto<?> body = templateService.findAll(0, 1).getBody();
            return body != null ? String.valueOf(body.getTotalElements()) : "0";
        } catch (Exception e) {
            log.warn("Unable to fetch template count for MMS dashboard: {}", e.getMessage());
            return I18n.t("mms.common.value.notAvailable");
        }
    }

}
