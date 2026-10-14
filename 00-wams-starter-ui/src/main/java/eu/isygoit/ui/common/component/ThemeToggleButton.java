package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.layout.PageEnvironment;

/**
 * Header action that switches between the Lumo light and dark themes by setting {@code theme="dark"} on the
 * document root (the attribute Lumo's colour scheme and the isygo theme listen to). The choice is remembered by
 * {@link PageEnvironment#storeTheme} and restored on every screen by {@link PageEnvironment#apply}.
 */
public class ThemeToggleButton extends Button {

    private boolean dark;

    public ThemeToggleButton() {
        addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        addClassNames("wams-header-icon-btn", "wams-theme-toggle");
        render();
        addClickListener(e -> toggle());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Reflect the theme the page actually applied (stored choice or OS preference).
        attachEvent.getUI().getPage()
                .executeJs("return document.documentElement.getAttribute('theme') === 'dark'")
                .then(Boolean.class, isDark -> {
                    dark = Boolean.TRUE.equals(isDark);
                    render();
                });
    }

    private void toggle() {
        dark = !dark;
        render();
        PageEnvironment.storeTheme(getUI().orElse(null), dark);
    }

    private void render() {
        setIcon((dark ? VaadinIcon.SUN_O : VaadinIcon.MOON_O).create());
        String label = dark
                ? I18n.t("common.layout.theme.light.tooltip")
                : I18n.t("common.layout.theme.dark.tooltip");
        setTooltipText(label);
        setAriaLabel(label);
    }
}
