package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe abstraite pour une section de statistiques.
 * Chaque section possède un titre, une ligne de cartes, et une méthode de chargement.
 */
public abstract class AbstractStatSection extends VerticalLayout {

    protected final List<StatCard> cards = new ArrayList<>();
    protected final String sectionTitleKey;

    public AbstractStatSection(String sectionTitleKey) {
        this.sectionTitleKey = sectionTitleKey;
        setSpacing(false);
        setPadding(false);
        setWidthFull();
        addClassName("wams-stats-section");

        H4 sectionTitle = new H4(I18n.t(sectionTitleKey));
        sectionTitle.addClassName("wams-stats-section-title");
        add(sectionTitle);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.addClassName("wams-stats-row");
        add(row);
    }

    /**
     * Crée une carte et l'ajoute à la section.
     */
    protected StatCard addCard(VaadinIcon icon, StatCard.Variant variant, String labelKey, String tooltipKey) {
        String label = I18n.t(labelKey);
        String tooltip = tooltipKey != null ? I18n.t(tooltipKey) : null;
        StatCard card = new StatCard(icon, variant, label, null, tooltip);
        cards.add(card);
        HorizontalLayout row = (HorizontalLayout) getComponentAt(1);
        row.add(card);
        return card;
    }

    /**
     * Charge les données de la section. À implémenter par chaque section.
     */
    public abstract void loadStatistics();

    /**
     * Helper pour définir la valeur d'une carte.
     */
    protected void setCardValue(StatCard card, String value) {
        card.setValue(value);
    }

    /**
     * Helper pour définir la tendance d'une carte.
     */
    protected void setCardTrend(StatCard card, String changeLabel, StatCard.Trend trend) {
        card.setChange(changeLabel, trend);
    }
}