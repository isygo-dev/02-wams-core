package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.dom.Element;
import eu.isygoit.i18n.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lightweight rich-text editor built on a {@code contenteditable} div.
 *
 * <p>Supports the formatting operations a message composer needs:
 * <b>bold</b>, <i>italic</i>, <u>underline</u>, <s>strikethrough</s>, text
 * color, font size, paragraph alignment, bullet / numbered lists, undo /
 * redo, and "clear formatting".
 *
 * <p>Value is exchanged as <b>HTML</b>. Callers that render the HTML outside
 * this editor must sanitize it — see
 * {@code MessageDetailsDialog#sanitizeHtml(String)} for the defensive
 * baseline used in this project.
 *
 * <p>The toolbar buttons carry an i18n tooltip resolved from
 * {@code rte.<action>}; if a key is missing the tooltip is simply omitted
 * rather than showing {@code !key!}.
 *
 * <p>Value-change tracking: the editor forwards every {@code input} and
 * {@code blur} event to the server as a custom {@code rte-change} event
 * carrying the current HTML. That keeps {@link #getValue()} synchronous
 * on the server — a form's save button click sees the latest content.
 *
 * <p>Because {@link Composite} does not implement {@code HasSize}, the
 * sizing methods used by call sites ({@link #setWidthFull()},
 * {@link #setWidth(String)}) are delegated to the root {@link Div}.
 */
public class RichTextEditor extends Composite<Div> {

    private static final String DEFAULT_MIN_HEIGHT = "220px";
    private static final String[] FONT_SIZES = {"12 px", "14 px", "16 px", "18 px", "24 px", "32 px"};

    private final Div editor;
    private final List<Consumer<String>> changeListeners = new ArrayList<>();
    private String value = "";

    public RichTextEditor() {
        Div root = getContent();
        root.addClassName("wams-rich-text-editor");

        editor = buildEditor();
        Div toolbar = buildToolbar();

        root.add(toolbar, editor);
        setMinHeight(DEFAULT_MIN_HEIGHT);
    }

    /* ══════════════════════════════════════════════════════════════
     * Public API
     * ══════════════════════════════════════════════════════════════ */

    public void setValue(String html) {
        this.value = html == null ? "" : html;
        editor.getElement().executeJs("this.innerHTML = $0 || ''", this.value);
    }

    public String getValue() {
        return value;
    }

    /** Plain text (HTML tags stripped) — useful for validation and search. */
    public String getPlainText() {
        return stripTags(value);
    }

    public boolean isEffectivelyEmpty() {
        return stripTags(value).isEmpty();
    }

    public void setLabel(String label) {
        Div root = getContent();
        // Drop any previous label to avoid duplicates on re-set.
        root.getChildren()
                .filter(c -> c.getElement().hasAttribute("data-rte-label"))
                .findFirst()
                .ifPresent(root::remove);

        if (label != null && !label.isBlank()) {
            Span labelSpan = new Span(label);
            labelSpan.addClassName("wams-rich-text-editor__label");
            labelSpan.getElement().setAttribute("data-rte-label", "true");
            root.addComponentAsFirst(labelSpan);
        }
    }

    public void setPlaceholder(String placeholder) {
        if (placeholder == null || placeholder.isBlank()) {
            editor.getElement().removeAttribute("data-placeholder");
        } else {
            editor.getElement().setAttribute("data-placeholder", placeholder);
        }
    }

    public void setMinHeight(String height) {
        editor.getStyle().set("min-height", height);
    }

    public void setMaxHeight(String height) {
        editor.getStyle().set("max-height", height);
        editor.getStyle().set("overflow-y", "auto");
    }

    public void setReadOnly(boolean readOnly) {
        editor.getElement().setAttribute("contenteditable", String.valueOf(!readOnly));
    }

    public void setRequiredIndicatorVisible(boolean visible) {
        getContent().getElement().getClassList().set("wams-rich-text-editor--required", visible);
    }

    public void addValueChangeListener(Consumer<String> listener) {
        if (listener != null) changeListeners.add(listener);
    }

    public void focus() {
        editor.getElement().executeJs("this.focus()");
    }

    /* ══════════════════════════════════════════════════════════════
     * Sizing delegation — mirrors HasSize methods used by call sites.
     * Composite does not implement HasSize, so we forward to the root.
     * ══════════════════════════════════════════════════════════════ */

    public void setWidthFull() {
        getContent().setWidthFull();
    }

    public void setWidth(String width) {
        getContent().setWidth(width);
    }

    public void setHeightFull() {
        getContent().setHeightFull();
    }

    public void setHeight(String height) {
        getContent().setHeight(height);
    }

    public void setSizeFull() {
        getContent().setSizeFull();
    }

    /* ══════════════════════════════════════════════════════════════
     * Editor + toolbar construction
     * ══════════════════════════════════════════════════════════════ */

    private Div buildEditor() {
        Div ed = new Div();
        ed.addClassName("wams-rich-text-editor__area");
        ed.getElement().setAttribute("contenteditable", "true");
        ed.getElement().setAttribute("spellcheck", "true");
        ed.getElement().setAttribute("role", "textbox");
        ed.getElement().setAttribute("aria-multiline", "true");

        // Client-side: forward every input / blur event as a custom event
        // carrying the current innerHTML so the server can keep `value` in
        // sync synchronously.
        ed.getElement().executeJs(
                "const el = this;" +
                        "if (!el.__rteWired) {" +
                        "  el.__rteWired = true;" +
                        "  const push = () => el.dispatchEvent(new CustomEvent('rte-change', { detail: el.innerHTML }));" +
                        "  el.addEventListener('input', push);" +
                        "  el.addEventListener('blur', push);" +
                        "}"
        );

        ed.getElement()
                .addEventListener("rte-change", e -> {
                    String html = e.getEventData().getString("event.detail");
                    value = html == null ? "" : html;
                    changeListeners.forEach(l -> l.accept(value));
                })
                .addEventData("event.detail");

        return ed;
    }

    private Div buildToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("wams-rich-text-editor__toolbar");

        toolbar.add(
                iconButton(VaadinIcon.BOLD, "rte.bold", () -> exec("bold")),
                iconButton(VaadinIcon.ITALIC, "rte.italic", () -> exec("italic")),
                iconButton(VaadinIcon.UNDERLINE, "rte.underline", () -> exec("underline")),
                strikethroughButton(),
                separator(),

                buildFontSizePicker(),
                buildColorPicker(),
                separator(),

                iconButton(VaadinIcon.ALIGN_LEFT, "rte.align.left", () -> exec("justifyLeft")),
                iconButton(VaadinIcon.ALIGN_CENTER, "rte.align.center", () -> exec("justifyCenter")),
                iconButton(VaadinIcon.ALIGN_RIGHT, "rte.align.right", () -> exec("justifyRight")),
                separator(),

                iconButton(VaadinIcon.LIST_UL, "rte.list.bullet", () -> exec("insertUnorderedList")),
                iconButton(VaadinIcon.LIST_OL, "rte.list.numbered", () -> exec("insertOrderedList")),
                separator(),

                iconButton(VaadinIcon.ARROW_LEFT, "rte.undo", () -> exec("undo")),
                iconButton(VaadinIcon.ARROW_RIGHT, "rte.redo", () -> exec("redo")),
                clearFormattingButton()
        );

        return toolbar;
    }

    private Button strikethroughButton() {
        Span strike = new Span("S");
        strike.getStyle().set("text-decoration", "line-through");
        strike.getStyle().set("font-weight", "600");
        Button btn = new Button(strike);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.addClassName("wams-rich-text-editor__btn");
        btn.getElement().setAttribute("tabindex", "-1");
        btn.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        btn.addClickListener(e -> exec("strikeThrough"));
        String tip = tooltip("rte.strikethrough");
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    private Button clearFormattingButton() {
        Button btn = new Button(VaadinIcon.ERASER.create());
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.addClassName("wams-rich-text-editor__btn");
        btn.getElement().setAttribute("tabindex", "-1");
        btn.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        btn.addClickListener(e -> {
            editor.getElement().executeJs(
                    "const el = this;" +
                            "document.execCommand('removeFormat', false, null);" +
                            "el.dispatchEvent(new CustomEvent('rte-change', { detail: el.innerHTML }));"
            );
        });
        String tip = tooltip("rte.clear");
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    private ComboBox<String> buildFontSizePicker() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setItems(FONT_SIZES);
        combo.setValue("14 px");
        combo.setAllowCustomValue(false);
        combo.setPlaceholder(tooltipOr("rte.size", "Size"));
        combo.addClassName("wams-rich-text-editor__font-size");
        combo.setWidth("100px");
        combo.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        combo.addValueChangeListener(e -> {
            if (e.getValue() == null) return;
            editor.getElement().executeJs(
                    "const el = this;" +
                            "const size = $0;" +
                            "const sel = window.getSelection();" +
                            "if (!sel || sel.rangeCount === 0) return;" +
                            "const range = sel.getRangeAt(0);" +
                            "if (range.collapsed) return;" +
                            "const span = document.createElement('span');" +
                            "span.style.fontSize = size;" +
                            "try { range.surroundContents(span); }" +
                            "catch (e) {" +
                            "  const frag = range.extractContents();" +
                            "  span.appendChild(frag);" +
                            "  range.insertNode(span);" +
                            "}" +
                            "el.dispatchEvent(new CustomEvent('rte-change', { detail: el.innerHTML }));",
                    e.getValue().replace(" ", "")
            );
        });
        return combo;
    }

    private Div buildColorPicker() {
        Div wrap = new Div();
        wrap.addClassName("wams-rich-text-editor__color-wrap");

        Element input = new Element("input");
        input.setAttribute("type", "color");
        input.setAttribute("value", "#000000");
        // Element has no addClassName; use the class-list API.
        input.getClassList().add("wams-rich-text-editor__color-input");
        String tip = tooltip("rte.color");
        if (!tip.isBlank()) input.setAttribute("title", tip);

        wrap.getElement().appendChild(input);

        input.executeJs(
                "this.addEventListener('input', () => {" +
                        "  this.dispatchEvent(new CustomEvent('rte-color', { detail: this.value }));" +
                        "});"
        );

        input.addEventListener("rte-color", e -> {
            String color = e.getEventData().getString("event.detail");
            if (color == null || color.isBlank()) return;
            editor.getElement().executeJs(
                    "document.execCommand('styleWithCSS', false, true);" +
                            "document.execCommand('foreColor', false, $0);" +
                            "this.dispatchEvent(new CustomEvent('rte-change', { detail: this.innerHTML }));",
                    color
            );
        }).addEventData("event.detail");

        return wrap;
    }

    /* ══════════════════════════════════════════════════════════════
     * Helpers
     * ══════════════════════════════════════════════════════════════ */

    private void exec(String command) {
        editor.getElement().executeJs(
                "document.execCommand('styleWithCSS', false, true);" +
                        "document.execCommand($0, false, null);" +
                        "this.dispatchEvent(new CustomEvent('rte-change', { detail: this.innerHTML }));",
                command
        );
    }

    private Button iconButton(VaadinIcon icon, String tooltipKey, Runnable onClick) {
        Button btn = new Button(icon.create());
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.addClassName("wams-rich-text-editor__btn");
        btn.getElement().setAttribute("tabindex", "-1");
        // Prevent the toolbar button from stealing focus from the editor.
        btn.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        btn.addClickListener(e -> onClick.run());
        String tip = tooltip(tooltipKey);
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    private static Span separator() {
        Span s = new Span();
        s.addClassName("wams-rich-text-editor__separator");
        return s;
    }

    private static String tooltip(String key) {
        try {
            String v = I18n.t(key);
            return v == null || v.equals(key) ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    private static String tooltipOr(String key, String fallback) {
        String v = tooltip(key);
        return v.isBlank() ? fallback : v;
    }

    private static String stripTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .trim();
    }
}