package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.html.Div;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only renderer for HTML produced by {@link RichTextEditor}.
 *
 * <p>Supports the editor's formatting and inline image data URLs. HTML is
 * sanitized before it is added to the DOM, so the component can be used to
 * render saved user-provided content safely.
 */
public class RichTextViewer extends Div {

    private static final Pattern IMAGE_DATA_URL = Pattern.compile(
            "^data:image/(?:jpeg|png|gif|webp);base64,[A-Za-z0-9+/]+={0,2}$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_VALUE = Pattern.compile(
            "^(?:#[0-9a-f]{3,8}|rgba?\\([0-9.%\\s,]+\\)|[a-z]{1,20})$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FONT_SIZE = Pattern.compile(
            "^([0-9]{1,2}(?:\\.[0-9]{1,2})?)(px|pt|em|rem|%)$",
            Pattern.CASE_INSENSITIVE);

    private static final Safelist RICH_TEXT = new Safelist()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "strike",
                    "span", "div", "blockquote", "ul", "ol", "li", "img")
            .addAttributes("span", "style")
            .addAttributes("div", "style")
            .addAttributes("p", "style")
            .addAttributes("img", "src", "alt")
            .addProtocols("img", "src", "data");

    private String value = "";

    public RichTextViewer() {
        addClassName("wams-rtv");
    }

    public RichTextViewer(String html) {
        this();
        setValue(html);
    }

    static String sanitizeHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        Document parsed = Jsoup.parseBodyFragment(html);
        parsed.select("[style]").forEach(element -> {
            String safeStyle = sanitizeStyle(element.attr("style"));
            if (safeStyle.isEmpty()) {
                element.removeAttr("style");
            } else {
                element.attr("style", safeStyle);
            }
        });

        Document cleaned = new Cleaner(RICH_TEXT).clean(parsed);
        cleaned.select("img").forEach(image -> {
            if (!IMAGE_DATA_URL.matcher(image.attr("src")).matches()) {
                image.remove();
            }
        });
        return cleaned.body().html();
    }

    private static String sanitizeStyle(String style) {
        Map<String, String> safeDeclarations = new LinkedHashMap<>();
        for (String declaration : style.split(";")) {
            int separator = declaration.indexOf(':');
            if (separator <= 0) {
                continue;
            }

            String property = declaration.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(separator + 1).trim();
            if (isSafeStyleValue(property, value)) {
                safeDeclarations.put(property, value);
            }
        }
        return safeDeclarations.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(java.util.stream.Collectors.joining("; "));
    }

    private static boolean isSafeStyleValue(String property, String value) {
        return switch (property) {
            case "color", "background-color" -> COLOR_VALUE.matcher(value).matches();
            case "text-align" -> value.matches("(?i)left|center|right|justify");
            case "font-size" -> isSafeFontSize(value);
            default -> false;
        };
    }

    private static boolean isSafeFontSize(String value) {
        Matcher matcher = FONT_SIZE.matcher(value);
        if (!matcher.matches()) {
            return false;
        }

        double size = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        return switch (unit) {
            case "px", "pt" -> size >= 6 && size <= 72;
            case "em", "rem" -> size >= 0.5 && size <= 4;
            case "%" -> size >= 50 && size <= 200;
            default -> false;
        };
    }

    /**
     * Returns the sanitized HTML currently displayed.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the HTML to display. Unsafe tags, attributes, styles, and image
     * sources are removed before rendering.
     */
    public void setValue(String html) {
        value = sanitizeHtml(html);
        getElement().setProperty("innerHTML", value);
    }
}
