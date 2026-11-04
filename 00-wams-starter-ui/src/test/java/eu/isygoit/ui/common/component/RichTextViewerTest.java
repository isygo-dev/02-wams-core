package eu.isygoit.ui.common.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextViewerTest {

    @Test
    void rendersEmbeddedImagesAndSafeFormatting() {
        String rendered = RichTextViewer.sanitizeHtml(
                "<p style=\"color: rgb(12, 34, 56); font-size: 18px; position: fixed\">"
                        + "<strong>Hello</strong><img src=\"data:image/jpeg;base64,SGVsbG8=\" "
                        + "onerror=\"alert(1)\"></p>");

        assertTrue(rendered.contains("<strong>Hello</strong>"));
        assertTrue(rendered.contains("src=\"data:image/jpeg;base64,SGVsbG8=\""));
        assertTrue(rendered.contains("color: rgb(12, 34, 56)"));
        assertFalse(rendered.contains("onerror"));
        assertFalse(rendered.contains("position"));
    }

    @Test
    void removesScriptsAndNonEmbeddedImages() {
        String rendered = RichTextViewer.sanitizeHtml(
                "<script>alert(1)</script><img src=\"https://example.com/image.png\">"
                        + "<img src=\"data:text/html;base64,PHNjcmlwdD4=\">");

        assertFalse(rendered.contains("<script"));
        assertFalse(rendered.contains("example.com"));
        assertFalse(rendered.contains("data:text/html"));
    }
}
