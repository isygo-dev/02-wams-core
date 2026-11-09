package eu.isygoit.dto.response;

/**
 * A resource used by an assignment.
 *
 * @param resourceId the resource
 * @param role       the requirement role it fulfils
 * @param amount     capacity consumed
 */
public record ResourceAssignment(String resourceId, String role, int amount) {
}
