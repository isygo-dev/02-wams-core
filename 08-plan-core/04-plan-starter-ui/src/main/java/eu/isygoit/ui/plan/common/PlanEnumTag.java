package eu.isygoit.ui.plan.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.enums.IEnumJobStatusCode;
import eu.isygoit.enums.IEnumLogLevel;
import eu.isygoit.enums.IEnumSolverStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.EnumTags;

import java.util.Locale;

/**
 * Plan entry point to the shared enum tag renderer ({@link EnumTags}): job statuses and log levels are shown as
 * the same coloured, translated tags as every other module. The tone of a tag is chosen by passing the name of
 * a constant the shared renderer already knows (for example {@code SUCCESS} for green), so no plan specific CSS
 * is needed.
 *
 * <ul>
 *   <li>SUCCEEDED: success</li>
 *   <li>FAILED: danger</li>
 *   <li>INFEASIBLE, STOPPING: warning</li>
 *   <li>CANCELLED: neutral</li>
 *   <li>CREATED, ACCEPTED, VALIDATING, NORMALIZING, SOLVING: info</li>
 * </ul>
 */
public final class PlanEnumTag {

    private static final EnumTags TAGS = new EnumTags("plan.enum", null);

    private PlanEnumTag() {
    }

    /**
     * Translated, coloured tag of a job status; {@code null} when the status is missing.
     */
    public static Span ofStatus(IEnumJobStatusCode.Types status) {
        return status == null ? null : TAGS.ofLabel(statusLabel(status), statusTone(status));
    }

    /**
     * Translated job status ({@code plan.ui.status.*}); a dash when missing.
     */
    public static String statusLabel(IEnumJobStatusCode.Types status) {
        return status == null ? "-" : I18n.t("plan.ui.status." + status.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Translated, coloured tag of a log level.
     */
    public static Span ofLevel(IEnumLogLevel.Types level) {
        return level == null ? null : TAGS.ofLabel(levelLabel(level), levelTone(level));
    }

    /**
     * Translated log level ({@code plan.ui.log.level.*}); a dash when missing.
     */
    public static String levelLabel(IEnumLogLevel.Types level) {
        return level == null ? "-" : I18n.t("plan.ui.log.level." + level.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Translated solver status ({@code plan.ui.solver.status.*}); a dash when missing.
     */
    public static String solverStatusLabel(IEnumSolverStatus.Types status) {
        return status == null ? "-" : I18n.t("plan.ui.solver.status." + status.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Read-only detail field (icon, label, any component) laid out like the shared details dialogs.
     */
    public static Component detailField(VaadinIcon icon, String label, Component value) {
        return TAGS.detailField(icon, label, value);
    }

    private static String statusTone(IEnumJobStatusCode.Types status) {
        return switch (status) {
            case SUCCEEDED -> "SUCCESS";
            case FAILED -> "FAILED";
            case INFEASIBLE, STOPPING -> "WARNING";
            case CANCELLED -> "CLOSED";
            case CREATED, ACCEPTED, VALIDATING, NORMALIZING, SOLVING -> "ASSIGNED";
        };
    }

    private static String levelTone(IEnumLogLevel.Types level) {
        return switch (level) {
            case ERROR -> "FAILED";
            case WARN -> "WARNING";
            case INFO -> "ASSIGNED";
            case DEBUG, TRACE -> "NONE";
        };
    }
}
