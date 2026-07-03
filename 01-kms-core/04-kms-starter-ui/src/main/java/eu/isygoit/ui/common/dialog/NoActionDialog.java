package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;

/**
 * Simple info dialog with a close button.
 * Content can be added via {@link #add(com.vaadin.flow.component.Component...)}.
 */
public class NoActionDialog extends Dialog {

    private final VerticalLayout contentWrapper;
    private final Button closeButton;

    public NoActionDialog(String title) {
        setHeaderTitle(title);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setWidth("500px");
        setMaxWidth("90%");
        setResizable(false);

        contentWrapper = new VerticalLayout();
        contentWrapper.setPadding(true);
        contentWrapper.setSpacing(true);
        contentWrapper.setWidthFull();

        // Add the wrapper as the main content
        super.add(contentWrapper);

        closeButton = new Button(I18n.t("common.dialog.noaction.close"), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(closeButton);
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footerLayout.setPadding(true);
        footerLayout.setSpacing(true);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }

    @Override
    public void add(com.vaadin.flow.component.Component... components) {
        contentWrapper.add(components);
    }
}