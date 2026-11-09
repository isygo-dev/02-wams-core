package eu.isygoit.service.impl.planning;

import eu.isygoit.dto.response.DiagnosticIssue;
import eu.isygoit.dto.response.ValidationReportDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collector of validation issues (design 7, all stages). Collects ALL problems instead of failing on the first one.
 * <p>
 * Identical issues (same severity, code, path and message) are stored once. Not thread-safe: one instance per
 * validation run.
 */
public final class ValidationResult {

    /** Severity of an error (the request cannot be solved). */
    public static final String ERROR = "ERROR";
    /** Severity of a warning (the request is solvable). */
    public static final String WARNING = "WARNING";

    /**
     * One issue.
     *
     * @param code     UPPER_SNAKE code of design 7.4
     * @param severity {@link #ERROR} or {@link #WARNING}
     * @param path     JSON path of the offending field (for example {@code tasks[3].predecessors[0]}), may be empty
     * @param message  English message
     * @param args     i18n placeholders (never {@code null}, insertion ordered)
     */
    public record Issue(String code, String severity, String path, String message, Map<String, Object> args) {
        public Issue {
            path = path == null ? "" : path;
            args = args == null || args.isEmpty()
                    ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }

        /** @return {@code true} for an error */
        public boolean isError() {
            return ERROR.equals(severity);
        }

        /** @return the transport form used by {@code POST /validate} */
        public ValidationReportDto.Issue toDto() {
            return ValidationReportDto.Issue.builder()
                    .code(code).severity(severity).message(message).path(path)
                    .args(new java.util.HashMap<>(args)).build();
        }

        /** @return the diagnostics form used inside solve results */
        public DiagnosticIssue toDiagnostic() {
            return new DiagnosticIssue(code, severity, path, message);
        }
    }

    private final List<Issue> issues = new ArrayList<>();
    private final Set<String> seen = new HashSet<>();

    /** Reports an error. */
    public ValidationResult error(String code, String path, String message) {
        return add(ERROR, code, path, message, null);
    }

    /** Reports an error with i18n arguments. */
    public ValidationResult error(String code, String path, String message, Map<String, Object> args) {
        return add(ERROR, code, path, message, args);
    }

    /** Reports a warning. */
    public ValidationResult warning(String code, String path, String message) {
        return add(WARNING, code, path, message, null);
    }

    /** Reports a warning with i18n arguments. */
    public ValidationResult warning(String code, String path, String message, Map<String, Object> args) {
        return add(WARNING, code, path, message, args);
    }

    /** Adds an already built issue (deduplicated). */
    public ValidationResult add(Issue issue) {
        return add(issue.severity(), issue.code(), issue.path(), issue.message(), issue.args());
    }

    private ValidationResult add(String severity, String code, String path, String message, Map<String, Object> args) {
        String key = severity + '\u0000' + code + '\u0000' + path + '\u0000' + message;
        if (seen.add(key)) {
            issues.add(new Issue(code, severity, path, message, args));
        }
        return this;
    }

    /** Appends every issue of another result. */
    public ValidationResult merge(ValidationResult other) {
        for (Issue issue : other.issues) {
            add(issue);
        }
        return this;
    }

    /** @return all issues in report order (unmodifiable view) */
    public List<Issue> issues() {
        return Collections.unmodifiableList(issues);
    }

    /** @return the errors in report order */
    public List<Issue> errors() {
        return issues.stream().filter(Issue::isError).toList();
    }

    /** @return the warnings in report order */
    public List<Issue> warnings() {
        return issues.stream().filter(i -> !i.isError()).toList();
    }

    /** @return {@code true} when at least one error was reported */
    public boolean hasErrors() {
        for (Issue issue : issues) {
            if (issue.isError()) {
                return true;
            }
        }
        return false;
    }

    /** @return the first error, or {@code null} when there is none */
    public Issue firstError() {
        for (Issue issue : issues) {
            if (issue.isError()) {
                return issue;
            }
        }
        return null;
    }

    /** @return the number of issues */
    public int size() {
        return issues.size();
    }
}
