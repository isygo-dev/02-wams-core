package eu.isygoit.deepseek.streaming;

/**
 * Callback interface for processing streaming tokens.
 */
@FunctionalInterface
public interface StreamListener {
    /**
     * Called each time a new token (or chunk) is received from the API.
     *
     * @param token the text chunk received
     */
    void onToken(String token);

    /**
     * Called when the stream completes successfully.
     */
    default void onComplete() {}

    /**
     * Called when an error occurs during streaming.
     *
     * @param throwable the error that occurred
     */
    default void onError(Throwable throwable) {}
}