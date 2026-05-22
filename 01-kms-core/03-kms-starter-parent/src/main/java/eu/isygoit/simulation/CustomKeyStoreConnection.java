package eu.isygoit.simulation;

import java.time.LocalDateTime;

public class CustomKeyStoreConnection {
    private final Long storeId;
    private final int timeoutSeconds;
    private LocalDateTime lastHeartbeat;

    public CustomKeyStoreConnection(Long storeId, LocalDateTime connectedAt, int timeoutSeconds) {
        this.storeId = storeId;
        this.lastHeartbeat = connectedAt;
        this.timeoutSeconds = timeoutSeconds;
    }

    public void refresh() {
        this.lastHeartbeat = LocalDateTime.now();
    }

    public boolean isExpired() {
        return lastHeartbeat.plusSeconds(timeoutSeconds).isBefore(LocalDateTime.now());
    }
}
