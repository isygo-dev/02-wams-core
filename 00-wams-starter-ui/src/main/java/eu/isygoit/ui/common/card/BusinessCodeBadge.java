package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ClipboardCopyButton;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Renders an entity's business code above the card title with a copy action.
 */
public final class BusinessCodeBadge {

    private BusinessCodeBadge() {
    }

    public static VerticalLayout prepend(HorizontalLayout header, String code) {
        if (header == null) {
            return null;
        }

        VerticalLayout stack = new VerticalLayout();
        stack.setPadding(false);
        stack.setSpacing(false);
        stack.setWidthFull();
        stack.addClassName("wams-card__code-stack");

        Component badge = of(code);
        if (badge != null) {
            stack.add(badge);
        }

        List<Component> title = header.getChildren().toList();
        header.removeAll();
        title.forEach(stack::add);
        header.add(stack);
        header.setFlexGrow(1, stack);
        return stack;
    }

    /**
     * Adds the badge only when the code is present, leaving the header unchanged otherwise.
     */
    public static void prependIfPresent(HorizontalLayout header, String code) {
        if (header != null && StringUtils.hasText(code)) {
            prepend(header, code);
        }
    }

    public static void update(VerticalLayout stack, String code) {
        if (stack == null) {
            return;
        }
        stack.getChildren()
                .filter(component -> component.getElement().getClassList().contains("wams-card__code-badge"))
                .findFirst()
                .ifPresent(stack::remove);
        Component badge = of(code);
        if (badge != null) {
            stack.addComponentAtIndex(0, badge);
        }
    }

    public static Component of(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        Span text = new Span(code);
        text.addClassName("wams-card__code-text");
        text.getElement().setAttribute("title", code);

        ClipboardCopyButton copyButton = new ClipboardCopyButton(
                code,
                I18n.t("common.code.copy.tooltip"),
                I18n.t("common.code.copy.success", code));

        Div badge = new Div(text, copyButton);
        badge.addClassName("wams-card__code-badge");
        return badge;
    }
}
