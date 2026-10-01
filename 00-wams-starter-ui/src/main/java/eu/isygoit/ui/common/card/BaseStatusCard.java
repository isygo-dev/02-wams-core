package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.List;

/**
 * Base card that displays a status tag on the <b>left</b> side of the footer,
 * with the action buttons right-aligned on the same row.
 *
 * <p>Subclasses must supply the status tag via {@link #buildStatusTag()} and the
 * action buttons via {@link #buildActionButtons()}. Returning {@code null} from
 * {@link #buildStatusTag()} keeps the footer layout identical to
 * {@link BaseCard} (actions right-aligned only).
 *
 * @param <V> the parent view type
 * @param <S> the service type used by this card
 */
public abstract class BaseStatusCard<V extends Component, S> extends BaseCard<V, S> {

    protected BaseStatusCard(V parentView, S objectService) {
        super(parentView, objectService);
    }

    /**
     * Build the status tag displayed at the footer's left side.
     * Return {@code null} to omit the status tag entirely.
     */
    protected abstract Span buildStatusTag();

    @Override
    protected void buildFooter() {
        Span statusTag = buildStatusTag();
        List<Button> buttons = buildActionButtons();

        if (statusTag == null && (buttons == null || buttons.isEmpty())) {
            footerRow = null;
            return;
        }

        // Right-aligned action bar
        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.setAlignItems(FlexComponent.Alignment.CENTER);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName("wams-card__button-bar");
        if (buttons != null) {
            buttons.forEach(buttonBar::add);
        }

        // Footer row: [ status tag | .... | button bar ]
        footerRow = new HorizontalLayout();
        footerRow.setWidthFull();
        footerRow.setSpacing(true);
        footerRow.setPadding(true);
        footerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        footerRow.addClassName("wams-card__footer-row");

        if (statusTag != null) {
            footerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            footerRow.add(statusTag);
        } else {
            footerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        }
        footerRow.add(buttonBar);
    }
}