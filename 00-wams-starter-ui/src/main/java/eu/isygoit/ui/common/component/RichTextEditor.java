package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.Component;
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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Modern, self-contained rich-text editor built on a {@code contenteditable}
 * div. Provides a grouped, hover-and-active-aware toolbar with the formatting
 * operations a message composer needs:
 *
 * <ul>
 *   <li>history — undo / redo</li>
 *   <li>inline format — bold / italic / underline / strikethrough</li>
 *   <li>block style — font size, text color, highlight</li>
 *   <li>alignment — left / center / right</li>
 *   <li>lists — bulleted / numbered</li>
 *   <li>media — inline images (optional, opt-in via
 *       {@link #setAllowInlineImages(boolean)})</li>
 *   <li>clear formatting</li>
 * </ul>
 *
 * <p><b>Active-state highlighting.</b> Bold / italic / underline / etc.
 * buttons light up when the cursor is inside text carrying that format.
 * The tracking runs entirely client-side
 * ({@code document.queryCommandState}) — no server roundtrips.
 *
 * <p><b>Value</b> is exchanged as <b>HTML</b>. Callers that render it
 * outside this editor must sanitize it — see
 * {@code MessageDetailsDialog#sanitizeHtml(String)}.
 *
 * <p><b>Inline images.</b> When enabled via {@link #setAllowInlineImages}:
 * picked images are client-side resized to ≤ {@value #MAX_IMAGE_DIMENSION}px
 * on the longest side, re-encoded as JPEG (q=0.85) and embedded directly as
 * base64 {@code data:} URLs. Rejected above {@link #setMaxImageBytes} to
 * keep the message body reasonable.
 *
 * <p><b>Selection preservation.</b> Clicking the color swatch or the font
 * size dropdown would normally steal focus and lose the text selection —
 * the editor stashes the current range on {@code mousedown} and restores it
 * before issuing the format command, so the operation always targets what
 * the user had selected.
 *
 * <p><b>Character count.</b> The status bar shows a live character count on
 * every keystroke. Optional soft-limit via {@link #setMaxCharacters(int)}.
 */
@Slf4j
public class RichTextEditor extends Composite<Div> {

    private static final String DEFAULT_MIN_HEIGHT = "220px";
    private static final String[] FONT_SIZES = {"12", "14", "16", "18", "20", "24", "32", "48"};
    private static final String DEFAULT_FONT_SIZE = "14";

    /** Longest side of an inserted image after client-side resize. */
    private static final int MAX_IMAGE_DIMENSION = 1200;

    /** Default rejection threshold on the base64 payload (~750 KB image). */
    private static final long DEFAULT_MAX_IMAGE_BYTES = 1_000_000L;

    /* DOM parts */
    private final Div root;
    private final Span labelSpan;
    private final Div toolbar;
    private final Div editor;
    private final Span charCount;
    private final Button imageBtn;

    /* State */
    private final Map<String, Button> formatButtons = new LinkedHashMap<>();
    private final List<Consumer<String>> changeListeners = new ArrayList<>();
    private String value = "";
    private boolean allowInlineImages = false;
    private long maxImageBytes = DEFAULT_MAX_IMAGE_BYTES;
    private int maxCharacters = 0;             // 0 = no limit

    public RichTextEditor() {
        root = getContent();
        root.addClassName("wams-rte");

        labelSpan = new Span();
        labelSpan.addClassName("wams-rte__label");
        labelSpan.setVisible(false);

        toolbar = new Div();
        toolbar.addClassName("wams-rte__toolbar");

        editor = buildEditor();
        charCount = new Span();
        charCount.addClassName("wams-rte__char-count");

        imageBtn = buildImageButton();

        buildToolbar();

        Div statusBar = new Div();
        statusBar.addClassName("wams-rte__status");
        statusBar.add(charCount);

        root.add(labelSpan, toolbar, editor, statusBar);
        editor.getStyle().set("min-height", DEFAULT_MIN_HEIGHT);

        wireActiveStateTracking();
        wireImagePicker();
        updateCharCount();
    }

    /* ══════════════════════════════════════════════════════════════
     * Public API — value & display
     * ══════════════════════════════════════════════════════════════ */

    public void setValue(String html) {
        this.value = html == null ? "" : html;
        editor.getElement().executeJs("this.innerHTML = $0 || ''", this.value);
        updateCharCount();
    }

    public String getValue() { return value; }

    public String getPlainText() { return stripTags(value); }

    public boolean isEffectivelyEmpty() { return stripTags(value).isEmpty(); }

    public void setLabel(String label) {
        if (label == null || label.isBlank()) {
            labelSpan.setVisible(false);
            labelSpan.setText("");
        } else {
            labelSpan.setText(label);
            labelSpan.setVisible(true);
        }
    }

    public void setPlaceholder(String placeholder) {
        if (placeholder == null || placeholder.isBlank()) {
            editor.getElement().removeAttribute("data-placeholder");
        } else {
            editor.getElement().setAttribute("data-placeholder", placeholder);
        }
    }

    public void setMinHeight(String height) { editor.getStyle().set("min-height", height); }

    public void setMaxHeight(String height) {
        editor.getStyle().set("max-height", height);
        editor.getStyle().set("overflow-y", "auto");
    }

    public void setReadOnly(boolean readOnly) {
        editor.getElement().setAttribute("contenteditable", String.valueOf(!readOnly));
        // Hide toolbar in read-only mode — no formatting actions available.
        toolbar.setVisible(!readOnly);
    }

    public void setRequiredIndicatorVisible(boolean visible) {
        // Div is a Component — the class list lives on its underlying Element.
        root.getElement().getClassList().set("wams-rte--required", visible);
    }

    public void setMaxCharacters(int max) {
        this.maxCharacters = Math.max(0, max);
        updateCharCount();
    }

    public void addValueChangeListener(Consumer<String> listener) {
        if (listener != null) changeListeners.add(listener);
    }

    public void focus() { editor.getElement().executeJs("this.focus()"); }

    /* Sizing delegation — Composite does not implement HasSize. */
    public void setWidthFull()      { getContent().setWidthFull(); }
    public void setWidth(String w)  { getContent().setWidth(w); }
    public void setHeightFull()     { getContent().setHeightFull(); }
    public void setHeight(String h) { getContent().setHeight(h); }
    public void setSizeFull()       { getContent().setSizeFull(); }

    /* ══════════════════════════════════════════════════════════════
     * Inline-image configuration
     * ══════════════════════════════════════════════════════════════ */

    public void setAllowInlineImages(boolean allow) {
        this.allowInlineImages = allow;
        imageBtn.setVisible(allow);
        root.getElement().executeJs("this.__rteAllowInline = $0", allow);
    }

    public boolean isAllowInlineImages() { return allowInlineImages; }

    public void setMaxImageBytes(long bytes) {
        this.maxImageBytes = bytes > 0 ? bytes : DEFAULT_MAX_IMAGE_BYTES;
        getContent().getElement().executeJs(
                "this.__rteMaxImageBytes = $0",
                (double) this.maxImageBytes);
    }

    /* ══════════════════════════════════════════════════════════════
     * DOM construction
     * ══════════════════════════════════════════════════════════════ */

    private Div buildEditor() {
        Div ed = new Div();
        ed.addClassName("wams-rte__editor");
        ed.getElement().setAttribute("contenteditable", "true");
        ed.getElement().setAttribute("spellcheck", "true");
        ed.getElement().setAttribute("role", "textbox");
        ed.getElement().setAttribute("aria-multiline", "true");

        // Client-side: forward every input / blur event carrying innerHTML
        // so the server value stays in sync.
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
                    updateCharCount();
                })
                .addEventData("event.detail");

        return ed;
    }

    private void buildToolbar() {
        toolbar.add(
                group(
                        iconButton(VaadinIcon.ARROW_LEFT, "rte.undo", "undo",
                                () -> exec("undo")),
                        iconButton(VaadinIcon.ARROW_RIGHT, "rte.redo", "redo",
                                () -> exec("redo"))
                ),
                group(
                        iconButton(VaadinIcon.BOLD, "rte.bold", "bold",
                                () -> exec("bold")),
                        iconButton(VaadinIcon.ITALIC, "rte.italic", "italic",
                                () -> exec("italic")),
                        iconButton(VaadinIcon.UNDERLINE, "rte.underline", "underline",
                                () -> exec("underline")),
                        strikethroughButton()
                ),
                group(
                        buildFontSizePicker(),
                        buildTextColorButton(),
                        buildHighlightButton()
                ),
                group(
                        iconButton(VaadinIcon.ALIGN_LEFT, "rte.align.left", "justifyLeft",
                                () -> exec("justifyLeft")),
                        iconButton(VaadinIcon.ALIGN_CENTER, "rte.align.center", "justifyCenter",
                                () -> exec("justifyCenter")),
                        iconButton(VaadinIcon.ALIGN_RIGHT, "rte.align.right", "justifyRight",
                                () -> exec("justifyRight"))
                ),
                group(
                        iconButton(VaadinIcon.LIST_UL, "rte.list.bullet", "insertUnorderedList",
                                () -> exec("insertUnorderedList")),
                        iconButton(VaadinIcon.LIST_OL, "rte.list.numbered", "insertOrderedList",
                                () -> exec("insertOrderedList"))
                ),
                group(imageBtn),
                group(
                        iconButton(VaadinIcon.ERASER, "rte.clear", "removeFormat",
                                () -> editor.getElement().executeJs(
                                        "document.execCommand('removeFormat', false, null);" +
                                                "this.dispatchEvent(new CustomEvent('rte-change', { detail: this.innerHTML }));"))
                )
        );
    }

    private Div group(Component... items) {
        Div g = new Div();
        g.addClassName("wams-rte__group");
        for (Component c : items) {
            if (c != null) g.add(c);
        }
        return g;
    }

    /* ══════════════════════════════════════════════════════════════
     * Toolbar items
     * ══════════════════════════════════════════════════════════════ */

    private Button iconButton(VaadinIcon icon, String tooltipKey, String cmd, Runnable onClick) {
        Button btn = new Button(icon.create());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btn.addClassName("wams-rte__btn");
        btn.getElement().setAttribute("tabindex", "-1");
        if (cmd != null) {
            btn.getElement().setAttribute("data-cmd", cmd);
            formatButtons.put(cmd, btn);
        }
        // Keep focus inside the editor — clicking a toolbar button must not
        // collapse the current selection.
        btn.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        btn.addClickListener(e -> onClick.run());
        String tip = tooltip(tooltipKey);
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    private Button strikethroughButton() {
        Span icon = new Span("S");
        icon.getStyle().set("text-decoration", "line-through");
        icon.getStyle().set("font-weight", "700");
        icon.getStyle().set("font-size", "14px");
        Button btn = new Button(icon);
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btn.addClassName("wams-rte__btn");
        btn.getElement().setAttribute("data-cmd", "strikeThrough");
        btn.getElement().setAttribute("tabindex", "-1");
        btn.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault())");
        btn.addClickListener(e -> exec("strikeThrough"));
        formatButtons.put("strikeThrough", btn);
        String tip = tooltip("rte.strikethrough");
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    private ComboBox<String> buildFontSizePicker() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setItems(FONT_SIZES);
        combo.setValue(DEFAULT_FONT_SIZE);
        combo.setAllowCustomValue(false);
        combo.setItemLabelGenerator(v -> v + " px");
        combo.addClassName("wams-rte__font-size");
        combo.getElement().setAttribute("tabindex", "-1");
        // Preserve selection when opening the dropdown.
        combo.getElement().executeJs(
                "this.addEventListener('mousedown', e => {" +
                        "  const root = this.closest('.wams-rte');" +
                        "  const editor = root && root.querySelector('.wams-rte__editor');" +
                        "  if (!editor) return;" +
                        "  const sel = window.getSelection();" +
                        "  if (sel && sel.rangeCount > 0) root.__rteSavedRange = sel.getRangeAt(0).cloneRange();" +
                        "});"
        );
        combo.addValueChangeListener(e -> {
            if (e.getValue() == null) return;
            editor.getElement().executeJs(
                    "const root = this.closest('.wams-rte');" +
                            "const sel = window.getSelection();" +
                            "if (root && root.__rteSavedRange) {" +
                            "  sel.removeAllRanges();" +
                            "  sel.addRange(root.__rteSavedRange);" +
                            "}" +
                            "document.execCommand('styleWithCSS', false, true);" +
                            "document.execCommand('fontSize', false, '7');" +
                            "const nodes = this.querySelectorAll('font[size=\"7\"]');" +
                            "nodes.forEach(f => {" +
                            "  const span = document.createElement('span');" +
                            "  span.style.fontSize = $0 + 'px';" +
                            "  span.innerHTML = f.innerHTML;" +
                            "  f.parentNode.replaceChild(span, f);" +
                            "});" +
                            "this.dispatchEvent(new CustomEvent('rte-change', { detail: this.innerHTML }));",
                    e.getValue()
            );
        });
        String tip = tooltip("rte.size");
        if (!tip.isBlank()) Tooltip.forComponent(combo).withText(tip);
        return combo;
    }

    private Div buildTextColorButton() {
        return buildColorSwatch("rte.color", "foreColor",
                VaadinIcon.FONT.create(), "#000000");
    }

    private Div buildHighlightButton() {
        Span icon = new Span("A");
        icon.addClassName("wams-rte__swatch-letter");
        return buildColorSwatch("rte.highlight", "hiliteColor",
                icon, "#ffff00");
    }

    /**
     * Builds an {@code Aa}-with-colored-underline swatch that opens the
     * native color picker. The native {@code <input type=color>} is
     * transparent and fills the swatch, so any click opens the picker.
     */
    private Div buildColorSwatch(String tooltipKey, String command,
                                 Component iconContent, String initialColor) {
        Div wrap = new Div();
        wrap.addClassName("wams-rte__swatch");

        Div iconWrap = new Div(iconContent);
        iconWrap.addClassName("wams-rte__swatch-icon");

        Div underline = new Div();
        underline.addClassName("wams-rte__swatch-underline");
        underline.getStyle().set("background-color", initialColor);

        wrap.add(iconWrap, underline);

        Element input = new Element("input");
        input.setAttribute("type", "color");
        input.setAttribute("value", initialColor);
        input.getClassList().add("wams-rte__swatch-input");
        wrap.getElement().appendChild(input);

        String tip = tooltip(tooltipKey);
        if (!tip.isBlank()) input.setAttribute("title", tip);

        // Preserve selection on focus loss.
        input.executeJs(
                "this.addEventListener('mousedown', () => {" +
                        "  const root = this.closest('.wams-rte');" +
                        "  const editor = root && root.querySelector('.wams-rte__editor');" +
                        "  if (!editor) return;" +
                        "  const sel = window.getSelection();" +
                        "  if (sel && sel.rangeCount > 0) root.__rteSavedRange = sel.getRangeAt(0).cloneRange();" +
                        "});"
        );

        // Client-side: update the underline swatch immediately.
        input.executeJs(
                "this.addEventListener('input', () => {" +
                        "  const wrap = this.closest('.wams-rte__swatch');" +
                        "  const underline = wrap && wrap.querySelector('.wams-rte__swatch-underline');" +
                        "  if (underline) underline.style.backgroundColor = this.value;" +
                        "  this.dispatchEvent(new CustomEvent('rte-color', { detail: this.value }));" +
                        "});"
        );

        final String cmd = command;
        input.addEventListener("rte-color", e -> {
            String color = e.getEventData().getString("event.detail");
            if (color == null || color.isBlank()) return;

            // "White" or transparent → treat as 'reset'.
            boolean reset = cmd.equals("hiliteColor") && "#ffffff".equalsIgnoreCase(color);
            String appliedColor = reset ? "transparent" : color;

            editor.getElement().executeJs(
                    "const root = this.closest('.wams-rte');" +
                            "const sel = window.getSelection();" +
                            "if (root && root.__rteSavedRange) {" +
                            "  sel.removeAllRanges();" +
                            "  sel.addRange(root.__rteSavedRange);" +
                            "}" +
                            "document.execCommand('styleWithCSS', false, true);" +
                            "document.execCommand($0, false, $1);" +
                            "this.dispatchEvent(new CustomEvent('rte-change', { detail: this.innerHTML }));",
                    cmd, appliedColor
            );
        }).addEventData("event.detail");

        return wrap;
    }

    private Button buildImageButton() {
        Button btn = new Button(VaadinIcon.PICTURE.create());
        btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        btn.addClassName("wams-rte__btn");
        btn.addClassName("wams-rte__image-btn");
        btn.getElement().setAttribute("tabindex", "-1");

        // 1. Force type="button" — dialogs are frequently wrapped in a <form>
        //    by the base class, and a submit button inside the form would fire
        //    the dialog's onOk() / close handler.
        btn.getElement().setAttribute("type", "button");

        // 2. Attach the picker trigger as a CAPTURE-phase click listener and
        //    fully stop the event. Without this, the click bubbles up to the
        //    Dialog's close-on-outside-click / OK handler and closes the modal
        //    before the native file picker even opens.
        btn.getElement().executeJs(
                "this.addEventListener('mousedown', e => {" +
                        "  e.preventDefault();" +
                        "  e.stopPropagation();" +
                        "}, { capture: true });" +
                        "this.addEventListener('click', e => {" +
                        "  e.preventDefault();" +
                        "  e.stopPropagation();" +
                        "  const root = this.closest('.wams-rte');" +
                        "  if (root && root.__rtePickImage) root.__rtePickImage();" +
                        "}, { capture: true });"
        );

        btn.setVisible(false);
        String tip = tooltip("rte.image");
        if (!tip.isBlank()) {
            Tooltip.forComponent(btn).withText(tip);
            btn.getElement().setAttribute("aria-label", tip);
        }
        return btn;
    }

    /* ══════════════════════════════════════════════════════════════
     * Active state tracking — highlight the buttons whose format
     * applies at the cursor. Entirely client-side.
     * ══════════════════════════════════════════════════════════════ */

    private void wireActiveStateTracking() {
        root.getElement().executeJs(
                "const root = this;" +
                        "if (root.__rteActiveWired) return;" +
                        "root.__rteActiveWired = true;" +
                        "const update = () => {" +
                        "  const editor = root.querySelector('.wams-rte__editor');" +
                        "  if (!editor || document.activeElement !== editor) return;" +
                        "  root.querySelectorAll('[data-cmd]').forEach(btn => {" +
                        "    const cmd = btn.getAttribute('data-cmd');" +
                        "    try {" +
                        "      const on = document.queryCommandState(cmd);" +
                        "      btn.toggleAttribute('data-active', !!on);" +
                        "    } catch (e) {}" +
                        "  });" +
                        "};" +
                        "const editor = root.querySelector('.wams-rte__editor');" +
                        "if (editor) {" +
                        "  editor.addEventListener('keyup', update);" +
                        "  editor.addEventListener('mouseup', update);" +
                        "  editor.addEventListener('focus', update);" +
                        "}" +
                        "document.addEventListener('selectionchange', () => {" +
                        "  const ed = root.querySelector('.wams-rte__editor');" +
                        "  if (ed && document.activeElement === ed) update();" +
                        "});"
        );
    }

    /* ══════════════════════════════════════════════════════════════
     * Inline image picker
     * ══════════════════════════════════════════════════════════════ */

    private void wireImagePicker() {
        root.getElement().executeJs(
                "const root = this;" +
                        "root.__rteMaxImageBytes = root.__rteMaxImageBytes || " + DEFAULT_MAX_IMAGE_BYTES + ";" +
                        "root.__rtePickImage = () => {" +
                        "  if (!root.__rteAllowInline) return;" +
                        "  const input = document.createElement('input');" +
                        "  input.type = 'file';" +
                        "  input.accept = 'image/*';" +
                        "  input.style.display = 'none';" +
                        "  document.body.appendChild(input);" +
                        "  input.onchange = () => {" +
                        "    const file = input.files && input.files[0];" +
                        "    document.body.removeChild(input);" +
                        "    if (!file) return;" +
                        "    const reader = new FileReader();" +
                        "    reader.onload = () => {" +
                        "      const probe = new Image();" +
                        "      probe.onload = () => {" +
                        "        const maxDim = " + MAX_IMAGE_DIMENSION + ";" +
                        "        let w = probe.naturalWidth, h = probe.naturalHeight;" +
                        "        if (w > maxDim || h > maxDim) {" +
                        "          const ratio = Math.min(maxDim / w, maxDim / h);" +
                        "          w = Math.round(w * ratio); h = Math.round(h * ratio);" +
                        "        }" +
                        "        const canvas = document.createElement('canvas');" +
                        "        canvas.width = w; canvas.height = h;" +
                        "        canvas.getContext('2d').drawImage(probe, 0, 0, w, h);" +
                        "        const dataUrl = canvas.toDataURL('image/jpeg', 0.85);" +
                        "        const payload = Math.floor((dataUrl.length - dataUrl.indexOf(',') - 1) * 3 / 4);" +
                        "        if (payload > root.__rteMaxImageBytes) {" +
                        "          console.warn('Inline image rejected: ' + payload + ' > ' + root.__rteMaxImageBytes);" +
                        "          return;" +
                        "        }" +
                        "        const editor = root.querySelector('.wams-rte__editor');" +
                        "        if (!editor) return;" +
                        "        editor.focus();" +
                        "        document.execCommand('insertImage', false, dataUrl);" +
                        "        editor.dispatchEvent(new CustomEvent('rte-change', { detail: editor.innerHTML }));" +
                        "      };" +
                        "      probe.onerror = () => console.warn('Cannot decode image');" +
                        "      probe.src = reader.result;" +
                        "    };" +
                        "    reader.readAsDataURL(file);" +
                        "  };" +
                        "  input.click();" +
                        "};"
        );
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

    private void updateCharCount() {
        int n = stripTags(value).length();
        String text = (maxCharacters > 0)
                ? n + " / " + maxCharacters + " " + I18n.t("rte.chars")
                : n + " " + I18n.t("rte.chars");
        charCount.setText(text);
        // Span is a Component — class list lives on its underlying Element.
        charCount.getElement().getClassList().set(
                "wams-rte__char-count--over",
                maxCharacters > 0 && n > maxCharacters);
    }

    private static String tooltip(String key) {
        try {
            String v = I18n.t(key);
            return v == null || v.equals(key) ? "" : v;
        } catch (Exception e) {
            return "";
        }
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