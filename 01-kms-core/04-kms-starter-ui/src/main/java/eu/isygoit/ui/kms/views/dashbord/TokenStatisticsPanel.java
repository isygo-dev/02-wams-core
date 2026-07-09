package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.common.component.StatCard;
import eu.isygoit.ui.common.component.StatCardGrid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class TokenStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(TokenStatisticsPanel.class);

    private final KmsTokenConfigService tokenConfigService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();

    private StatCard totalCard;
    private StatCard accessCard;
    private StatCard refreshCard;
    private StatCard rstpwdCard;
    private StatCard authorityCard;

    public TokenStatisticsPanel(KmsTokenConfigService tokenConfigService, UI ui) {
        this.tokenConfigService = tokenConfigService;
        this.ui = ui;
        buildUI();
        loadStatistics();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(false);
        setWidthFull();

        HorizontalLayout titleRow = new HorizontalLayout();
        titleRow.setWidthFull();
        titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
        H3 title = new H3(I18n.t("kms.stats.token.title"));
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(title, loadingBar);
        add(titleRow);

        totalCard = new StatCard(VaadinIcon.COG, StatCard.Variant.NEUTRAL, I18n.t("kms.stats.token.total"), null, I18n.t("kms.stats.token.total.tooltip"));
        accessCard = new StatCard(VaadinIcon.KEY, StatCard.Variant.PRIMARY, I18n.t("kms.stats.token.access"), null, I18n.t("kms.stats.token.access.tooltip"));
        refreshCard = new StatCard(VaadinIcon.REFRESH, StatCard.Variant.PRIMARY, I18n.t("kms.stats.token.refresh"), null, I18n.t("kms.stats.token.refresh.tooltip"));
        rstpwdCard = new StatCard(VaadinIcon.LOCK, StatCard.Variant.WARNING, I18n.t("kms.stats.token.rstpwd"), null, I18n.t("kms.stats.token.rstpwd.tooltip"));
        authorityCard = new StatCard(VaadinIcon.USER, StatCard.Variant.PRIMARY, I18n.t("kms.stats.token.authority"), null, I18n.t("kms.stats.token.authority.tooltip"));

        add(new StatCardGrid(totalCard, accessCard, refreshCard, rstpwdCard, authorityCard));
    }

    public void loadStatistics() {
        ui.access(() -> {
            loadingBar.setVisible(true);

            try {
                TokenStats stats = new TokenStats();

                ResponseEntity<PaginatedResponseDto<TokenConfigDto>> totalResp = tokenConfigService.findAll(0, 1);
                PaginatedResponseDto<TokenConfigDto> totalBody = totalResp.getBody();
                if (totalBody != null) stats.total = totalBody.getTotalElements();

                ResponseEntity<List<TokenConfigDto>> listResp = tokenConfigService.findAllList();
                if (listResp.getBody() != null && listResp.getBody() != null) {
                    List<TokenConfigDto> configs = listResp.getBody();
                    stats.access = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.ACCESS).count();
                    stats.refresh = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.REFRESH).count();
                    stats.rstpwd = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.RSTPWD).count();
                    stats.authority = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.AUTHORITY).count();
                }

                totalCard.setValue(String.valueOf(stats.total));
                accessCard.setValue(String.valueOf(stats.access));
                refreshCard.setValue(String.valueOf(stats.refresh));
                rstpwdCard.setValue(String.valueOf(stats.rstpwd));
                authorityCard.setValue(String.valueOf(stats.authority));

                loadingBar.setVisible(false);
                ui.push();

            } catch (Exception e) {
                log.error("Error fetching token configuration statistics", e);
                loadingBar.setVisible(false);
                Notification.show(I18n.t("kms.stats.token.load.error"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }

    private static class TokenStats {
        long total = 0, access = 0, refresh = 0, rstpwd = 0, authority = 0;
    }
}