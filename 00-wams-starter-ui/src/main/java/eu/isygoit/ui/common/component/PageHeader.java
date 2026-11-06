package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import eu.isygoit.i18n.I18n;

/**
 * Canonical page header used by every routed view: the page title and a one-line
 * description of what the page is for. Styling comes from the shared
 * {@code administration-header} / {@code administration-subtitle} rules in
 * {@code styles/scss/components/_enterprise.scss}, so views built on
 * {@code AbstractPaginatedView} and custom views look identical.
 */
@CssImport("./styles/scss/common.scss")
public class PageHeader extends Div {

    public PageHeader(String titleKey, String subtitleKey) {
        addClassName("administration-header");
        add(new H2(I18n.t(titleKey)));
        if (subtitleKey != null && !subtitleKey.isBlank()) {
            Paragraph subtitle = new Paragraph(I18n.t(subtitleKey));
            subtitle.addClassName("administration-subtitle");
            add(subtitle);
        }
    }
}
