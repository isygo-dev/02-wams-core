package eu.isygoit.ui.kms.views.dashbord;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.common.PaginatedResponseDto;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TokenStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(TokenStatisticsPanel.class);

    private final KmsTokenConfigService tokenConfigService;
    private final UI ui;
    private final ProgressBar loadingBar = new ProgressBar();
    private HorizontalLayout statsContainer;

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
        H3 title = new H3("Token Configuration Statistics");
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        titleRow.add(title, loadingBar);
        add(titleRow);

        statsContainer = new HorizontalLayout();
        statsContainer.setWidthFull();
        statsContainer.setSpacing(true);
        statsContainer.getStyle().set("flex-wrap", "wrap").set("gap", "16px");
        add(statsContainer);
        showPlaceholderCards();
    }

    private void showPlaceholderCards() {
        statsContainer.removeAll();
        statsContainer.add(
                new StatCard("Total Configs", "…", VaadinIcon.COG, "#607D8B", "Total number of JWT token configurations"),
                new StatCard("ACCESS", "…", VaadinIcon.KEY, "#1E88E5", "Configurations for access tokens (used for API authorization)"),
                new StatCard("REFRESH", "…", VaadinIcon.REFRESH, "#43A047", "Configurations for refresh tokens (used to obtain new access tokens)"),
                new StatCard("RSTPWD", "…", VaadinIcon.LOCK, "#F57C00", "Configurations for password reset tokens"),
                new StatCard("AUTHORITY", "…", VaadinIcon.USER, "#8E24AA", "Configurations for authority tokens (granting specific permissions)")
        );
    }

    public void loadStatistics() {
        loadingBar.setVisible(true);
        CompletableFuture.supplyAsync(() -> {
            TokenStats stats = new TokenStats();
            try {
                ResponseEntity<PaginatedResponseDto<TokenConfigDto>> totalResp = tokenConfigService.findAll(0, 1);
                PaginatedResponseDto<TokenConfigDto> totalBody = totalResp.getBody();
                if (totalBody != null) stats.total = totalBody.getTotalElements();

                ResponseEntity<PaginatedResponseDto<TokenConfigDto>> listResp = tokenConfigService.findAll(0, 500);
                if (listResp.getBody() != null && listResp.getBody().getContent() != null) {
                    List<TokenConfigDto> configs = listResp.getBody().getContent();
                    stats.access = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.ACCESS).count();
                    stats.refresh = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.REFRESH).count();
                    stats.rstpwd = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.RSTPWD).count();
                    stats.authority = configs.stream().filter(c -> c.getTokenType() == IEnumToken.Types.AUTHORITY).count();
                }
            } catch (Exception e) {
                log.error("Error fetching token configuration statistics", e);
            }
            return stats;
        }).orTimeout(30, TimeUnit.SECONDS).exceptionally(ex -> {
            log.error("Token stats timeout/failure", ex);
            return new TokenStats();
        }).thenAccept(stats -> {
            UI updateUi = ui != null ? ui : UI.getCurrent();
            if (updateUi == null) return;
            updateUi.access(() -> {
                statsContainer.removeAll();
                statsContainer.add(
                        new StatCard("Total Configs", String.valueOf(stats.total), VaadinIcon.COG, "#607D8B", "Total number of JWT token configurations"),
                        new StatCard("ACCESS", String.valueOf(stats.access), VaadinIcon.KEY, "#1E88E5", "Configurations for access tokens (used for API authorization)"),
                        new StatCard("REFRESH", String.valueOf(stats.refresh), VaadinIcon.REFRESH, "#43A047", "Configurations for refresh tokens (used to obtain new access tokens)"),
                        new StatCard("RSTPWD", String.valueOf(stats.rstpwd), VaadinIcon.LOCK, "#F57C00", "Configurations for password reset tokens"),
                        new StatCard("AUTHORITY", String.valueOf(stats.authority), VaadinIcon.USER, "#8E24AA", "Configurations for authority tokens (granting specific permissions)")
                );
                loadingBar.setVisible(false);
                updateUi.push();
            });
        });
    }

    private static class TokenStats {
        long total = 0, access = 0, refresh = 0, rstpwd = 0, authority = 0;
    }
}