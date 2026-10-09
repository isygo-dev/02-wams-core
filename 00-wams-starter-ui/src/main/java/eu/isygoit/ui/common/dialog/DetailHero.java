package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Read-only header of a details dialog: avatar (picture or placeholder), name,
 * a row of chips (code, gender, status...) and a subtitle (usually the email).
 */
public class DetailHero extends HorizontalLayout {

    public DetailHero(String imageSrc, String name, String subtitle, Component... chips) {
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        addClassName(DialogLayout.CLASS_HERO);

        Div avatar = new Div();
        avatar.addClassName(DialogLayout.CLASS_AVATAR);
        if (imageSrc != null && !imageSrc.isBlank()) {
            Image image = new Image(imageSrc, "");
            image.setWidthFull();
            image.setHeightFull();
            image.addClassName(DialogLayout.CLASS_AVATAR_IMG);
            avatar.add(image);
        } else {
            Icon placeholder = VaadinIcon.USER.create();
            placeholder.addClassName(DialogLayout.CLASS_AVATAR_ICON);
            avatar.add(placeholder);
        }

        Span nameSpan = new Span(name);
        nameSpan.addClassName(DialogLayout.CLASS_HERO_NAME);

        HorizontalLayout chipRow = new HorizontalLayout(chips);
        chipRow.setSpacing(true);
        chipRow.addClassName(DialogLayout.CLASS_HERO_CHIPS);

        Span subtitleSpan = new Span(subtitle == null ? "" : subtitle);
        subtitleSpan.addClassName(DialogLayout.CLASS_HERO_SUBTITLE);

        VerticalLayout info = new VerticalLayout(nameSpan, chipRow, subtitleSpan);
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();
        info.addClassName(DialogLayout.CLASS_HERO_INFO);

        add(avatar, info);
        expand(info);
    }
}
