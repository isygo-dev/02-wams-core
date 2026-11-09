package eu.isygoit.dto.response;

import eu.isygoit.util.CollectionCopies;
import eu.isygoit.util.ImmutableJsonMap;

import java.util.List;
import java.util.Map;

/**
 * Explanations attached to a solve result.
 *
 * @param message            human readable summary
 * @param infeasibilityCore  legacy flat list of ids (constraint ids and task ids), kept for CSV/UI
 * @param core               structured IIS-style core, null when feasible
 * @param warnings           non-fatal observations
 * @param details            free-form details (insertion ordered)
 */
public record Diagnostics(
        String message,
        List<String> infeasibilityCore,
        InfeasibilityCore core,
        List<DiagnosticIssue> warnings,
        Map<String, Object> details) {

    public Diagnostics {
        infeasibilityCore = CollectionCopies.list(infeasibilityCore);
        warnings = CollectionCopies.list(warnings);
        details = ImmutableJsonMap.copyOf(details);
    }
}
