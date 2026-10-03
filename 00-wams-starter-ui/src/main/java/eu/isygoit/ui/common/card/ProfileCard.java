package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import org.springframework.util.StringUtils;

/**
 * Common base for cards that display a profile-like DTO carrying:
 * <ul>
 *   <li>an avatar / image</li>
 *   <li>a full display name and an optional subtitle (e.g. email)</li>
 *   <li>a status tag (rendered at the footer's left by {@link BaseStatusCard})</li>
 *   <li>a set of enum tags (rendered at the bottom of the body, just above
 *       the footer, via {@link BaseCard#buildEnumTags()}; the tags row wraps
 *       on narrow cards)</li>
 * </ul>
 *
 * @param <V> parent view type
 * @param <S> service type
 * @param <D> DTO type displayed by the card
 */
public abstract class ProfileCard<V extends Component, S, D> extends BaseStatusCard<V, S> {

    /** The DTO being displayed. Subclasses can read it directly. */
    protected final D profile;

    protected ProfileCard(V parentView, S objectService, D profile) {
        super(parentView, objectService);
        this.profile = profile;
    }

    /* ══════════════════════════════════════════════════════════════
     * Contract — subclasses expose profile info
     * ══════════════════════════════════════════════════════════════ */

    /** Path/URL to the profile picture. May be {@code null}/{@code blank} → user icon. */
    protected abstract String profileImagePath();

    /** Full display name, e.g. {@code "Mme Yasmine Trabelsi"}. Never {@code null}. */
    protected abstract String profileFullName();

    /** Secondary line under the name, e.g. email. Return {@code null} to skip. */
    protected abstract String profileSubtitle();

    /** Status chip shown at the footer's left. Return {@code null} to skip. */
    protected abstract Span profileStatusTag();

    @Override
    protected final Span buildStatusTag() {
        return profileStatusTag();
    }

    /* ══════════════════════════════════════════════════════════════
     * Default header — avatar + (full name over subtitle)
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected Component buildTitle() {
        HorizontalLayout hero = new HorizontalLayout();
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.setSpacing(true);
        hero.setWidthFull();
        hero.addClassName("wams-profile-card__hero");

        Component avatar = buildProfileAvatar();
        Component info   = buildProfileNameBlock();

        hero.add(avatar, info);
        hero.expand(info);
        return hero;
    }

    /** Round avatar. Uses {@link #profileImagePath()} when non-blank, else a user icon. */
    protected Component buildProfileAvatar() {
        Div wrap = new Div();
        wrap.addClassName("wams-profile-card__avatar-wrap");

        String path = profileImagePath();
        if (StringUtils.hasText(path)) {
            Image img = new Image(path, "");
            img.setWidth("100%");
            img.setHeight("100%");
            img.addClassName("wams-profile-card__avatar-img");
            wrap.add(img);
        } else {
            Icon icon = VaadinIcon.USER.create();
            icon.addClassName("wams-profile-card__avatar-icon");
            wrap.add(icon);
        }
        return wrap;
    }

    /** Full name on top, optional subtitle underneath — stacked as a single flex item. */
    protected Component buildProfileNameBlock() {
        VerticalLayout info = new VerticalLayout();
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();
        info.addClassName("wams-profile-card__name-block");

        String fullName = profileFullName();
        Span nameSpan = buildTitleSpan(fullName, fullName);
        nameSpan.addClassName("wams-profile-card__full-name");
        info.add(nameSpan);

        String subtitle = profileSubtitle();
        if (StringUtils.hasText(subtitle)) {
            Span sub = new Span(subtitle);
            sub.addClassName("wams-profile-card__subtitle");
            info.add(sub);
        }
        return info;
    }

    /* ══════════════════════════════════════════════════════════════
     * Shared body helpers
     * ══════════════════════════════════════════════════════════════ */

    /** Standard body row: {@code [icon] Label: value}. */
    protected HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("wams-profile-card__row");

        Icon iconComponent = icon.create();
        iconComponent.addClassName("wams-profile-card__row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName("wams-profile-card__row-label");

        Span valueSpan = new Span(value == null || value.isBlank() ? "-" : value);
        valueSpan.addClassName("wams-profile-card__row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    /**
     * Convenience wrapper for {@link BaseCard#buildEnumTags()}: collects the
     * supplied tag spans into one {@link HorizontalLayout}.
     *
     * <p>The row <b>wraps</b> on narrow cards — tags spill onto the next line
     * instead of overflowing horizontally. Returns {@code null} if there is
     * nothing to display.
     */
    protected HorizontalLayout buildEnumTagsRow(Span... tags) {
        if (tags == null || tags.length == 0) {
            return null;
        }
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.addClassName("wams-profile-card__tags-row");

        for (Span tag : tags) {
            if (tag != null) {
                tag.addClassName("wams-profile-card__tag");
                row.add(tag);
            }
        }
        return row.getComponentCount() == 0 ? null : row;
    }

    /**
     * Builds an enum chip with i18n lookup, falling back to the exact enum
     * constant name when no key is registered.
     *
     * <p>Key convention: {@code <keyPrefix>.<enum-name-lowercase>}.
     *
     * @param enumValue  the enum constant (nullable → returns null)
     * @param keyPrefix  e.g. {@code "student.gender"}
     * @param color      chip color
     * @param tooltipKey optional i18n key for the tooltip (nullable)
     */
    protected Span buildEnumChip(Enum<?> enumValue, String keyPrefix, ChipColor color, String tooltipKey) {
        if (enumValue == null) {
            return null;
        }
        String key = keyPrefix + "." + enumValue.name();
        String label = translate(key, enumValue);
        Span chip = buildStatusChip(label, color);

        String tooltip = tooltipKey != null ? I18n.t(tooltipKey) : null;
        chip.getElement().setAttribute("title",
                tooltip != null && !tooltip.equals(tooltipKey) ? tooltip : label);
        return chip;
    }

    /**
     * Resolves a translation key; if missing, falls back to the enum constant
     * name through the shared enum translation key.
     */
    protected static String translate(String key, Enum<?> enumValue) {
        String value = I18n.t(key);
        if (value == null || value.isBlank() || value.equals(key) || value.equals("!" + key + "!")) {
            return I18n.enumLabel(enumValue);
        }
        return value;
    }
}
