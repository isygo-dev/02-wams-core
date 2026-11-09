package eu.isygoit.async.camel.processor;

/**
 * A Kafka record that can never be processed (empty or structurally invalid). Not retryable: the Camel routes send
 * it straight to the dead-letter topic.
 */
public class PoisonMessageException extends RuntimeException {
    public PoisonMessageException(String message) {
        super(message);
    }
}
