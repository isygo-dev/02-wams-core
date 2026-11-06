package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstraite pour un panneau de statistiques.
 * Gère l'en-tête commun (titre, rafraîchissement, chargement) et délègue
 * la construction des sections et le chargement des données aux sous-classes.
 * <p>
 * Toutes les dimensions (gap, padding, marges, largeurs) sont pilotées par
 * les classes CSS déclarées dans common.scss :
 * .wams-stats-panel, .wams-stats-header, .wams-stats-actions,
 * .wams-stats-title, .wams-stats-loader
 */
@CssImport("./styles/scss/common.scss")
public abstract class AbstractStatisticsPanel extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AbstractStatisticsPanel.class);

    protected final UI ui;
    protected final ProgressBar loadingBar = new ProgressBar();
    private final List<Runnable> extraRefreshActions = new ArrayList<>();
    protected final FloatingRefreshButton refreshFab = new FloatingRefreshButton(this::onRefreshRequested);

    public AbstractStatisticsPanel(UI ui) {
        this.ui = ui;
        buildUI();
        buildSections();
    }

    private void buildUI() {
        // Lumo theme flags — structural, no measurements. All sizing is in SCSS.
        setSpacing(false);
        setPadding(false);
        setMargin(false);
        setWidthFull();
        addClassName("wams-stats-panel");

        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        headerLayout.addClassName("wams-stats-header");

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        actionsLayout.setSpacing(false);
        actionsLayout.addClassName("wams-stats-actions");

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.addClassName("wams-stats-loader");
        actionsLayout.add(loadingBar);

        if (StringUtils.hasText(getTitleKey())) {
            headerLayout.addClassName("has-title");
            H3 statsTitle = new H3(getTitleKey());
            statsTitle.addClassName("wams-stats-title");
            headerLayout.add(statsTitle, actionsLayout);
        } else {
            // No title: refresh sits at the far right, no dead space in the row.
            headerLayout.addClassName("no-title");
            headerLayout.add(actionsLayout);
        }

        add(headerLayout, refreshFab);
    }

    /**
     * Registers another reload to run when the page's single floating refresh button is clicked
     * (for a sibling panel on the same page that has no refresh button of its own).
     */
    public void addRefreshAction(Runnable action) {
        extraRefreshActions.add(action);
    }

    private void onRefreshRequested() {
        loadStatistics();
        extraRefreshActions.forEach(Runnable::run);
    }

    /**
     * Charge les statistiques de manière asynchrone avec gestion du push UI.
     */
    public void loadStatistics() {
        ui.access(() -> {
            loadingBar.setVisible(true);
            refreshFab.setLoading(true);

            try {
                log.info("Chargement des statistiques pour {}", getClass().getSimpleName());
                doLoadStatistics();
                loadingBar.setVisible(false);
                refreshFab.setLoading(false);
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
        refreshFab.setLoading(false);
        Notification.show(I18n.t("common.stats.load.error"), 3000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}