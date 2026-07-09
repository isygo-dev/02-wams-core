package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;

/**
 * Simple info dialog with a close button.
 * Content can be added via {@link #add(com.vaadin.flow.component.Component...)}.
 *
 * <p>Same footer contract as {@link BaseActionDialog}: an error-message slot
 * (hidden until {@link #showError(String)} is called) sits to the left of the
 * action button(s), so every dialog in the app — action or read-only — has an
 * identical footer shape.
 */
public class NoActionDialog extends Dialog {

    private final VerticalLayout contentWrapper;
    private final Button closeButton;
    private final Span errorSpan;

    public NoActionDialog(String title) {
        setHeaderTitle(title);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        setWidth("500px");
        setMaxWidth("90%");
        setResizable(false);
        addClassName("wams-dialog-responsive");

        contentWrapper = new VerticalLayout();
        contentWrapper.setPadding(true);
        contentWrapper.setSpacing(true);
        contentWrapper.setWidthFull();

        // Add the wrapper as the main content
        super.add(contentWrapper);

        errorSpan = new Span();
        errorSpan.addClassName(LumoUtility.TextColor.ERROR);
        errorSpan.addClassName(LumoUtility.FontSize.SMALL);
        errorSpan.addClassName("wams-dialog-error-span");
        errorSpan.setVisible(false);

        closeButton = new Button(I18n.t("common.dialog.noaction.close"), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout footerLayout = new HorizontalLayout(errorSpan, closeButton);
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        footerLayout.setPadding(true);
        footerLayout.setSpacing(true);

        getFooter().removeAll();
        getFooter().add(footerLayout);
    }

    /**
     * Shows an error message in the footer (e.g. when the dialog's data failed
     * to load) — same shape/position as {@link BaseActionDialog#showError(String)}.
     */
    protected void showError(String message) {
        errorSpan.setText(message);
        errorSpan.setVisible(message != null && !message.isBlank());
    }

    protected void clearError() {
        errorSpan.setText(null);
        errorSpan.setVisible(false);
    }

    @Override
    public void add(com.vaadin.flow.component.Component... components) {
        contentWrapper.add(components);
    }
}