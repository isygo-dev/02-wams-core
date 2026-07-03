package eu.isygoit.util;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.i18n.I18n;

/**
 * Utility class pour faciliter la création de composants UI avec i18n intégré.
 * Fournit des méthodes helper pour créer des composants avec traductions automatiques.
 */
public class I18nUIHelper {

    /**
     * Crée un Button avec traduction automatique
     */
    public static Button createButton(String i18nKey) {
        return new Button(I18n.t(i18nKey));
    }

    /**
     * Crée un Button avec traduction et icône
     */
    public static Button createButton(String i18nKey, VaadinIcon icon) {
        return new Button(I18n.t(i18nKey), icon.create());
    }

    /**
     * Crée un H1 avec traduction
     */
    public static H1 createH1(String i18nKey) {
        return new H1(I18n.t(i18nKey));
    }

    /**
     * Crée un H2 avec traduction
     */
    public static H2 createH2(String i18nKey) {
        return new H2(I18n.t(i18nKey));
    }

    /**
     * Crée un H3 avec traduction
     */
    public static H3 createH3(String i18nKey) {
        return new H3(I18n.t(i18nKey));
    }

    /**
     * Crée un H4 avec traduction
     */
    public static H4 createH4(String i18nKey) {
        return new H4(I18n.t(i18nKey));
    }

    /**
     * Crée un H5 avec traduction
     */
    public static H5 createH5(String i18nKey) {
        return new H5(I18n.t(i18nKey));
    }

    /**
     * Crée un H6 avec traduction
     */
    public static H6 createH6(String i18nKey) {
        return new H6(I18n.t(i18nKey));
    }

    /**
     * Crée un Span avec traduction
     */
    public static Span createSpan(String i18nKey) {
        return new Span(I18n.t(i18nKey));
    }

    /**
     * Crée un TextField avec placeholder traduit
     */
    public static TextField createTextField(String placeholderKey) {
        TextField field = new TextField();
        field.setPlaceholder(I18n.t(placeholderKey));
        return field;
    }

    /**
     * Crée un TextField avec label et placeholder traduits
     */
    public static TextField createTextField(String labelKey, String placeholderKey) {
        TextField field = new TextField();
        field.setLabel(I18n.t(labelKey));
        field.setPlaceholder(I18n.t(placeholderKey));
        return field;
    }

    /**
     * Définit le texte d'un Button avec traduction
     */
    public static void setText(Button button, String i18nKey) {
        button.setText(I18n.t(i18nKey));
    }

    /**
     * Définit le placeholder d'un TextField avec traduction
     */
    public static void setPlaceholder(TextField field, String i18nKey) {
        field.setPlaceholder(I18n.t(i18nKey));
    }

    /**
     * Définit le label d'un TextField avec traduction
     */
    public static void setLabel(TextField field, String i18nKey) {
        field.setLabel(I18n.t(i18nKey));
    }

    /**
     * Définit le tooltip d'un composant avec traduction
     */
    public static <T extends com.vaadin.flow.component.Component> T setTooltip(T component, String i18nKey) {
        component.getElement().setProperty("title", I18n.t(i18nKey));
        return component;
    }

    /**
     * Obtient un message d'erreur traduit
     */
    public static String getErrorMessage(String errorType) {
        return I18n.t("common.error." + errorType);
    }

    /**
     * Obtient un message de succès traduit
     */
    public static String getSuccessMessage(String type) {
        return I18n.t("common.success." + type);
    }

    /**
     * Obtient un message de notification traduit
     */
    public static String getNotificationMessage(String type) {
        return I18n.t("common.notification." + type);
    }

    /**
     * Obtient un message de validation traduit
     */
    public static String getValidationMessage(String type) {
        return I18n.t("common.validation." + type);
    }
}

