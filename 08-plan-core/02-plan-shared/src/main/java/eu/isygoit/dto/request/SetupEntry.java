package eu.isygoit.dto.request;

/**
 * Sparse override of a {@link SetupMatrix}: setup ticks needed when a task of class {@code from} is immediately
 * followed by a task of class {@code to} on the same resource.
 * <pre>{@code
 * { "from": "red", "to": "blue", "ticks": 12 }
 * }</pre>
 *
 * @param from  setup class of the preceding task
 * @param to    setup class of the following task
 * @param ticks idle ticks between the END of the first and the START of the second, {@code >= 0}
 */
public record SetupEntry(String from, String to, int ticks) {
}
