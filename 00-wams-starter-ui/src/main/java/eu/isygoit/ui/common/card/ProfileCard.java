package eu.isygoit.ui.common.card;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import eu.isygoit.async.ContextAwareAsync;
import eu.isygoit.i18n.I18n;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;

/**
 * Common base for cards that display a profile-like DTO carrying:
 * <ul>
 *   <li>an avatar / image</li>
 *   <li>a full display name and an optional subtitle (e.g. email)</li>
 *   <li>a status tag (rendered at the footer's left by {@link BaseStatusCard})</li>
 *   <li>a set of enum tags (rendered at the bottom of the body via
 *       {@link BaseCard#buildEnumTags()})</li>
 * </ul>
 *
 * <h3>Profile image</h3>
 * The avatar is always rendered as an {@link Image}. Its source is resolved in
 * this order:
 * <ol>
 *   <li>{@link #profileImagePath()} — used directly when non-blank;</li>
 *   <li>otherwise {@link #profileImagePlaceholder()} is shown;</li>
 *   <li>and if the subclass provides a non-null {@link #profileImageLoader()},
 *       {@link #onCardAttach(AttachEvent)} dispatches it asynchronously via
 *       {@link ContextAwareAsync} and calls {@link #setProfileImageBytes(byte[])}
 *       once the bytes are available.</li>
 * </ol>
 *
 * @param <V> parent view type
 * @param <S> service type
 * @param <D> DTO type displayed by the card
 */
public abstract class ProfileCard<V extends Component, S, D> extends BaseStatusCard<V, S> {

    /**
     * Default data-URI SVG placeholder (generic user silhouette) shown when
     * no real image is available.
     */
    protected static final String DEFAULT_PROFILE_IMAGE_PLACEHOLDER =
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' " +
                    "viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' " +
                    "stroke-linecap='round' stroke-linejoin='round'%3E" +
                    "%3Ccircle cx='12' cy='8' r='4'%3E%3C/circle%3E" +
                    "%3Cpath d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3C/svg%3E";

    /**
     * The DTO being displayed. Subclasses can read it directly.
     */
    protected final D profile;

    /**
     * The avatar image element. Kept as a field so subclasses can update the
     * source asynchronously via {@link #setProfileImageBytes(byte[])}.
     */
    protected Image profileAvatarImage;

    protected ProfileCard(V parentView, S objectService, D profile) {
        super(parentView, objectService);
        this.profile = profile;
    }

    /* ══════════════════════════════════════════════════════════════
     * Contract — subclasses expose profile info
     * ══════════════════════════════════════════════════════════════ */

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

    /**
     * Path/URL to the profile picture. May be {@code null}/{@code blank}.
     * <p>Cards that <em>download</em> the image (see
     * {@link #profileImageLoader()}) typically return {@code null} here.
     */
    protected abstract String profileImagePath();

    /**
     * Full display name, e.g. {@code "Mme Yasmine Trabelsi"}. Never {@code null}.
     */
    protected abstract String profileFullName();

    /**
     * Secondary line under the name, e.g. email. Return {@code null} to skip.
     */
    protected abstract String profileSubtitle();

    /**
     * Status chip shown at the footer's left. Return {@code null} to skip.
     */
    protected abstract Span profileStatusTag();

    /* ══════════════════════════════════════════════════════════════
     * Default header — avatar + (full name over subtitle)
     * ══════════════════════════════════════════════════════════════ */

    @Override
    protected final Span buildStatusTag() {
        return profileStatusTag();
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout hero = new HorizontalLayout();
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.setSpacing(true);
        hero.setWidthFull();
        hero.addClassName("wams-profile-card__hero");

        Component avatar = buildProfileAvatar();
        Component info = buildProfileNameBlock();

        hero.add(avatar, info);
        hero.expand(info);
        return hero;
    }

    /**
     * Builds the round avatar as an {@link Image} so its source can be swapped
     * asynchronously. Initial source is {@link #profileImagePath()} when
     * non-blank, otherwise {@link #profileImagePlaceholder()}.
     */
    protected Component buildProfileAvatar() {
        Div wrap = new Div();
        wrap.addClassName("wams-profile-card__avatar-wrap");

        profileAvatarImage = new Image();
        profileAvatarImage.setWidth("100%");
        profileAvatarImage.setHeight("100%");
        profileAvatarImage.addClassName("wams-profile-card__avatar-img");

        String path = profileImagePath();
        profileAvatarImage.setSrc(StringUtils.hasText(path) ? path : profileImagePlaceholder());

        wrap.add(profileAvatarImage);
        return wrap;
    }

    /* ══════════════════════════════════════════════════════════════
     * Async image loading — hooks for subclasses
     * ══════════════════════════════════════════════════════════════ */

    /**
     * Placeholder shown when no real image is available yet. Defaults to a
     * generic user silhouette. Subclasses may override.
     */
    protected String profileImagePlaceholder() {
        return DEFAULT_PROFILE_IMAGE_PLACEHOLDER;
    }

    /**
     * Hook for subclasses that want to <em>download</em> the profile image
     * (rather than relying on a static path). Default returns {@code null}.
     *
     * <p>When non-null, {@link #onCardAttach(AttachEvent)} dispatches the
     * loader through {@link ContextAwareAsync#supplyAsync(java.util.function.Supplier)},
     * i.e. on a background executor that has the Spring Security context,
     * request attributes and Vaadin session propagated. Implementations may
     * therefore perform blocking I/O (e.g. an authenticated Feign call)
     * safely off the UI thread.
     *
     * <p>The returned bytes are pushed back onto the UI thread and consumed by
     * {@link #setProfileImageBytes(byte[])}.
     *
     * @see #setProfileImageBytes(byte[])
     */
    protected ProfileImageLoader profileImageLoader() {
        return null;
    }

    /**
     * Replaces the avatar source with the given bytes. Passing {@code null}
     * or an empty array resets the avatar to
     * {@link #profileImagePlaceholder()}.
     *
     * <p>Must be invoked on the UI thread.
     */
    protected final void setProfileImageBytes(byte[] bytes) {
        if (profileAvatarImage == null) {
            return;
        }
        if (bytes != null && bytes.length > 0) {
            StreamResource resource = new StreamResource(
                    "profile-image-" + System.identityHashCode(profile),
                    () -> new ByteArrayInputStream(bytes));
            profileAvatarImage.setSrc(resource);
        } else {
            profileAvatarImage.setSrc(profileImagePlaceholder());
        }
    }

    /**
     * Default attach hook: dispatches {@link #profileImageLoader()} on the
     * {@link ContextAwareAsync} executor (so the Spring Security context,
     * request attributes and Vaadin session are propagated), then pushes the
     * resulting bytes back onto the UI thread via {@code ui.access(...)}.
     *
     * <p>If the loader is absent, throws, or returns no bytes, the avatar
     * keeps/falls back to {@link #profileImagePlaceholder()}.
     *
     * <p>Subclasses overriding this method <b>must</b> call
     * {@code super.onCardAttach(event)}.
     */
    @Override
    protected void onCardAttach(AttachEvent event) {
        ProfileImageLoader loader = profileImageLoader();
        if (loader == null) {
            return;
        }
        getUI().ifPresent(ui ->
                ContextAwareAsync.supplyAsync(() -> {
                            try {
                                return loader.load();
                            } catch (Exception e) {
                                // Fall back to the placeholder on any failure.
                                return null;
                            }
                        })
                        .whenComplete((bytes, error) ->
                                ui.access(() -> setProfileImageBytes(error == null ? bytes : null)))
        );
    }

    /* ══════════════════════════════════════════════════════════════
     * Shared body helpers
     * ══════════════════════════════════════════════════════════════ */

    /**
     * Full name on top, optional subtitle underneath — stacked as a single flex item.
     */
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

    /**
     * Standard body row: {@code [icon] Label: value}.
     */
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
     * Convenience wrapper for {@link BaseCard#buildEnumTags()}.
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

    /* ══════════════════════════════════════════════════════════════
     * Functional hook
     * ══════════════════════════════════════════════════════════════ */

    /**
     * Strategy for fetching profile-image bytes asynchronously.
     *
     * <p>Invoked by {@link #onCardAttach(AttachEvent)} through
     * {@link ContextAwareAsync#supplyAsync(java.util.function.Supplier)}, i.e.
     * on a background executor that has the Spring Security context, request
     * attributes and Vaadin session propagated. Implementations may therefore
     * perform blocking I/O (e.g. an authenticated Feign call) safely.
     *
     * <p>The returned bytes are pushed back to the UI on the UI thread and
     * consumed by {@link #setProfileImageBytes(byte[])}.
     */
    @FunctionalInterface
    public interface ProfileImageLoader {
        /**
         * @return raw image bytes, or {@code null}/empty to show the
         * placeholder instead.
         */
        byte[] load() throws Exception;
    }
}