package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Basic confirmation dialog for an action that has no data to show or edit:
 * warning message, optional PIN, Cancel / action button. It is never tabbed and
 * always uses the compact width. Used directly for enable / disable / toggle /
 * revoke style actions, and through {@link DeleteActionDialog} for deletions.
 *
 * <p>PIN check, loading state, HTTP-status handling, error extraction and feedback
 * behave the same for every module.
 */
@Slf4j
public class PinConfirmActionDialog extends PinBaseActionDialog {

    /** The remote call; may return a {@link ResponseEntity}, whose non-2xx status is then treated as a failure. */
    @FunctionalInterface
    public interface Action {
        Object run() throws Exception;
    }

    /**
     * Localised texts of one action.
     *
     * @param title       dialog title
     * @param message     warning message (already formatted with the entity name)
     * @param button      label of the action button
     * @param invalidCode message shown when the PIN does not match
     * @param success     message shown after success
     * @param error       builds the failure message from the technical detail
     */
    public record Texts(String title,
                        String message,
                        String button,
                        String invalidCode,
                        String success,
                        Function<String, String> error) {
    }

    private final Texts texts;
    private final Action action;
    private final Consumer<Boolean> loading;

    /**
     * @param texts       localised texts
     * @param action      the remote call
     * @param onSuccess   refresh callback, run once by the base dialog after success
     * @param loading     shows / hides the parent view loading bar; may be {@code null}
     * @param moduleClass CSS class of the owning module (for example {@code "ims-dialog"}); may be {@code null}
     * @param requirePin  {@code false} when the confirmation message alone is enough
     * @param variant     button style (error for destructive actions, primary otherwise)
     */
    public PinConfirmActionDialog(Texts texts,
                                  Action action,
                                  Runnable onSuccess,
                                  Consumer<Boolean> loading,
                                  String moduleClass,
                                  boolean requirePin,
                                  ButtonVariant variant) {
        super(texts.title(), texts.message(), onSuccess, requirePin);
        this.texts = texts;
        this.action = action;
        this.loading = loading;
        if (moduleClass != null && !moduleClass.isBlank()) {
            addClassName(moduleClass);
        }
        setOkButtonText(texts.button());
        addThemeVariantsOkButton(variant);
        DialogLayout.size(this, DialogLayout.WIDTH_S);
    }

    /** Adds a secondary note (for example an irreversibility reminder) under the warning message. */
    protected final void addNote(String note) {
        addContent(DialogLayout.help(note));
    }

    @Override
    protected boolean onOk() {
        if (!validatePin()) {
            append(texts.invalidCode());
            return false;
        }

        showLoading(true);
        try {
            Object result = action.run();
            if (result instanceof ResponseEntity<?> response && !response.getStatusCode().is2xxSuccessful()) {
                append(texts.error().apply("HTTP " + response.getStatusCode().value()));
                return false;
            }
            append(texts.success());
            return true;
        } catch (FeignException ex) {
            log.error("Action failed: {}", ex.getMessage(), ex);
            append(texts.error().apply(feignMessage(ex)));
            return false;
        } catch (Exception ex) {
            log.error("Action failed", ex);
            append(texts.error().apply(ex.getMessage()));
            return false;
        } finally {
            showLoading(false);
        }
    }

    private void showLoading(boolean visible) {
        if (loading != null) {
            loading.accept(visible);
        }
    }

    private static String feignMessage(FeignException ex) {
        try {
            String body = ex.contentUTF8();
            if (body != null && !body.isBlank()) {
                return body;
            }
        } catch (Exception ignored) {
            // fall back to the exception message
        }
        return ex.getMessage();
    }
}
