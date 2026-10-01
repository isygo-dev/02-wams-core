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
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.enums.IEnum;
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
        hero.addClassName("card__title-row");
        hero.getStyle().set("gap", "14px");
        hero.getStyle().set("flex-wrap", "nowrap");

        Component avatar = buildProfileAvatar();
        Component info   = buildProfileNameBlock();

        hero.add(avatar, info);
        hero.expand(info);
        return hero;
    }

    /** Round avatar. Uses {@link #profileImagePath()} when non-blank, else a user icon. */
    protected Component buildProfileAvatar() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("width", "72px")
                .set("height", "72px")
                .set("border-radius", "50%")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("flex-shrink", "0")
                .set("align-self", "center")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border", "3px solid var(--lumo-primary-color-50pct)")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.06)");

        String path = profileImagePath();
        if (StringUtils.hasText(path)) {
            Image img = new Image(path, "");
            img.setWidth("100%");
            img.setHeight("100%");
            img.getStyle().set("object-fit", "cover");
            wrap.add(img);
        } else {
            Icon icon = VaadinIcon.USER.create();
            icon.setSize("34px");
            icon.getStyle().set("color", "var(--lumo-contrast-40pct)");
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
        info.getStyle().set("gap", "4px");
        info.getStyle().set("min-width", "0");

        String fullName = profileFullName();
        Span nameSpan = buildTitleSpan(fullName, fullName);
        nameSpan.addClassName("card__title");
        nameSpan.getStyle().set("line-height", "1.2");
        nameSpan.getStyle().set("white-space", "normal");
        info.add(nameSpan);

        String subtitle = profileSubtitle();
        if (StringUtils.hasText(subtitle)) {
            Span sub = new Span(subtitle);
            sub.addClassName(LumoUtility.FontSize.XSMALL);
            sub.addClassName(LumoUtility.TextColor.SECONDARY);
            sub.getStyle().set("word-break", "break-all");
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
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.addClassName("card__row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("card__row-label");

        Span valueSpan = new Span(value == null || value.isBlank() ? "-" : value);
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("card__row-value");
        valueSpan.getStyle().set("word-break", "break-word");

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
        row.addClassName("card__tags-row");
        row.getStyle()
                .set("flex-wrap", "wrap")
                .set("row-gap", "var(--lumo-space-xs)")
                .set("column-gap", "var(--lumo-space-s)");
        for (Span tag : tags) {
            if (tag != null) {
                tag.getStyle().set("flex-shrink", "0");
                row.add(tag);
            }
        }
        return row.getComponentCount() == 0 ? null : row;
    }

    /**
     * Builds an enum chip with i18n lookup, falling back to the enum's
     * {@link IEnum#meaning()} when no translation key is registered.
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
        String key = keyPrefix + "." + enumValue.name().toLowerCase();
        String label = translate(key, enumValue);
        Span chip = buildStatusChip(label, color);

        String tooltip = tooltipKey != null ? I18n.t(tooltipKey) : null;
        chip.getElement().setAttribute("title",
                tooltip != null && !tooltip.equals(tooltipKey) ? tooltip : label);
        return chip;
    }

    /**
     * Resolves a translation key; if missing (translation equals the key or is
     * blank), falls back to the enum's {@code meaning()}.
     */
    protected static String translate(String key, Enum<?> enumValue) {
        String value = I18n.t(key);
        if (value == null || value.isBlank() || value.equals(key)) {
            if (enumValue instanceof IEnum ienum) {
                String meaning = ienum.meaning();
                return meaning != null ? meaning : enumValue.name();
            }
            return enumValue.name();
        }
        return value;
    }
}