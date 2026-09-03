package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.i18n.I18n;
import eu.isygoit.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classe abstraite pour un panneau de statistiques.
 * Gère l'en-tête commun (titre, rafraîchissement, chargement) et délègue
 * la construction des sections et le chargement des données aux sous-classes.
 */
@CssImport("./styles/card.css")
public abstract class AbstractStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AbstractStatisticsPanel.class);

    protected final UI ui;
    protected final ProgressBar loadingBar = new ProgressBar();
    protected final Button refreshButton = new Button(I18n.t("common.stats.refresh.button"), VaadinIcon.REFRESH.create());

    public AbstractStatisticsPanel(UI ui) {
        this.ui = ui;
        buildUI();
        buildSections();
        loadStatistics();
    }

    private void buildUI() {
        setSpacing(true);
        setPadding(false);
        setWidthFull();
        addClassName("wams-stats-panel");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.addClassName("wams-stats-header");

        H3 statsTitle = new H3(getTitleKey());
        statsTitle.addClassName("wams-stats-title");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionsLayout.setSpacing(true);

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClassName("wams-stats-refresh");
        refreshButton.addClickListener(e -> loadStatistics());

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("150px");
        loadingBar.addClassName("wams-stats-loader");

        actionsLayout.add(refreshButton, loadingBar);
        headerLayout.add(statsTitle, actionsLayout);
        add(headerLayout);
    }

    /**
     * Charge les statistiques de manière asynchrone avec gestion du push UI.
     */
    public void loadStatistics() {
        ui.access(() -> {
            loadingBar.setVisible(true);
            refreshButton.setEnabled(false);

            try {
                log.info("Chargement des statistiques pour {}", getClass().getSimpleName());
                doLoadStatistics();
                loadingBar.setVisible(false);
                refreshButton.setEnabled(true);
                ui.push();
            } catch (Exception ex) {
                log.error("Erreur lors du chargement des statistiques", ex);
                handleError();
            }
        });
    }

    /**
     * Effectue le chargement effectif des données.
     */
    protected abstract void doLoadStatistics();

    /**
     * Construit les sections de statistiques (à appeler dans le constructeur).
     */
    protected abstract void buildSections();

    /**
     * Retourne la clé de traduction du titre du panneau.
     */
    protected abstract String getTitleKey();

    private void handleError() {
        loadingBar.setVisible(false);
        refreshButton.setEnabled(true);
        Notification.show(I18n.t("common.stats.load.error"), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}