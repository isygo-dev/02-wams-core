package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import org.springframework.util.StringUtils;

/**
 * Read-only header of a details dialog: avatar (picture or placeholder), name,
 * a row of chips (code, gender, status...) and a subtitle (usually the email).
 *
 * <p>The avatar is always rendered as an {@link Image} so its source can be
 * replaced after construction — useful when the bytes are downloaded
 * asynchronously (or synchronously before opening, in the case of
 * {@code *DetailsViewDialog}). Priority order:
 * <ol>
 *   <li>{@link StreamResource} passed to the extended constructor or to
 *       {@link #setImage(StreamResource)};</li>
 *   <li>static {@code imagePath} passed to either constructor;</li>
 *   <li>built-in user-silhouette SVG placeholder.</li>
 * </ol>
 */
public class DetailHero extends HorizontalLayout {

    /**
     * Neutral user silhouette shown when neither bytes nor a path is available.
     */
    private static final String DEFAULT_AVATAR_PLACEHOLDER =
            "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='96' height='96' " +
                    "viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' " +
                    "stroke-linecap='round' stroke-linejoin='round'%3E" +
                    "%3Ccircle cx='12' cy='8' r='4'%3E%3C/circle%3E" +
                    "%3Cpath d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3C/svg%3E";

    /**
     * The avatar image element — kept as a field so its src can be swapped.
     */
    private final Image avatarImage;

    /**
     * The name span — kept so {@link #setFullName(String)} can update it.
     */
    private final Span nameSpan;

    /**
     * The subtitle span — kept so {@link #setSubtitle(String)} can update it.
     */
    private final Span subtitleSpan;

    /* ══════════════════════════════════════════════════════════════
     * Constructors
     * ══════════════════════════════════════════════════════════════ */

    /**
     * Preferred constructor: uses {@code imageResource} when non-null; otherwise
     * falls back to {@code imagePath} (then to the placeholder).
     *
     * @param imageResource downloaded avatar bytes (may be {@code null})
     * @param imagePath     fallback static path/URL (may be {@code null})
     * @param fullName      primary name line
     * @param subtitle      optional secondary line, e.g. email
     * @param chips         zero or more chips appended to the right block
     */
    public DetailHero(StreamResource imageResource,
                      String imagePath,
                      String fullName,
                      String subtitle,
                      Component... chips) {
        this(imagePath, fullName, subtitle, chips);
        if (imageResource != null) {
            this.avatarImage.setSrc(imageResource);
        }
    }

    /**
     * Basic constructor. Uses {@code imageSrc} as the avatar source when
     * non-blank, otherwise the built-in placeholder.
     *
     * @param imageSrc static path/URL to the avatar (may be {@code null})
     * @param name     primary name line (never {@code null}; blanks render "-")
     * @param subtitle optional secondary line, e.g. email (may be {@code null})
     * @param chips    zero or more chips appended to the right block
     */
    public DetailHero(String imageSrc, String name, String subtitle, Component... chips) {
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        addClassName(DialogLayout.CLASS_HERO);

        // ── Avatar ────────────────────────────────────────────────
        Div avatar = new Div();
        avatar.addClassName(DialogLayout.CLASS_AVATAR);

        avatarImage = new Image();
        avatarImage.setWidthFull();
        avatarImage.setHeightFull();
        avatarImage.addClassName(DialogLayout.CLASS_AVATAR_IMG);
        avatarImage.setAlt("");
        avatarImage.setSrc(StringUtils.hasText(imageSrc) ? imageSrc : DEFAULT_AVATAR_PLACEHOLDER);
        avatar.add(avatarImage);

        // ── Right block: name / chips / subtitle ──────────────────
        nameSpan = new Span(safe(name));
        nameSpan.addClassName(DialogLayout.CLASS_HERO_NAME);

        HorizontalLayout chipRow = new HorizontalLayout();
        chipRow.setSpacing(true);
        chipRow.addClassName(DialogLayout.CLASS_HERO_CHIPS);
        chipRow.getStyle().set("flex-wrap", "wrap");
        if (chips != null) {
            for (Component chip : chips) {
                if (chip != null) {
                    chipRow.add(chip);
                }
            }
        }

        subtitleSpan = new Span(subtitle == null ? "" : subtitle);
        subtitleSpan.addClassName(DialogLayout.CLASS_HERO_SUBTITLE);

        VerticalLayout info = new VerticalLayout(nameSpan, chipRow, subtitleSpan);
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();
        info.addClassName(DialogLayout.CLASS_HERO_INFO);

        add(avatar, info);
        expand(info);
    }

    /* ══════════════════════════════════════════════════════════════
     * Public API — swap content after construction
     * ══════════════════════════════════════════════════════════════ */

    private static String safe(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    /**
     * Replaces the avatar source with the given resource. Passing {@code null}
     * resets it to the built-in placeholder.
     */
    public void setImage(StreamResource imageResource) {
        if (imageResource != null) {
            avatarImage.setSrc(imageResource);
        } else {
            avatarImage.setSrc(DEFAULT_AVATAR_PLACEHOLDER);
        }
    }

    /**
     * Replaces the avatar source with a static path/URL. Passing {@code null}
     * or blank resets it to the built-in placeholder.
     */
    public void setImagePath(String imagePath) {
        if (StringUtils.hasText(imagePath)) {
            avatarImage.setSrc(imagePath);
        } else {
            avatarImage.setSrc(DEFAULT_AVATAR_PLACEHOLDER);
        }
    }

    /**
     * Updates the primary name line. Blanks render as "-".
     */
    public void setFullName(String fullName) {
        nameSpan.setText(safe(fullName));
    }

    /* ══════════════════════════════════════════════════════════════
     * Helpers
     * ══════════════════════════════════════════════════════════════ */

    /**
     * Updates the secondary line. Passing {@code null}/blank clears it.
     */
    public void setSubtitle(String subtitle) {
        subtitleSpan.setText(subtitle == null ? "" : subtitle);
    }
}