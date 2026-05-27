package eu.isygoit.quartz.service;

import eu.isygoit.enums.IEnumKeyExpirationModel;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.Tenant;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.TenantRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for sending notifications about keys that are about to expire.
 * This job is scheduled to run daily (e.g., at 09:00) via Quartz.
 */
@Slf4j
@Service
public class KeyExpiryNotificationService extends AbstractJobService {

    @Autowired
    private KmsKeyRepository kmsKeyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    //@Autowired(required = false) // optional if you have a notification service
    //private INotificationService notificationService;

    @Value("${kms.key.expiry.warning.days:7}") // configurable warning period, default 7 days
    private int warningDays;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {
        log.info("Starting key expiry notification check...");

        // Fetch all tenants
        List<Tenant> tenants = tenantRepository.findAll();
        if (CollectionUtils.isEmpty(tenants)) {
            log.warn("No tenants found, skipping expiry notification check");
            return;
        }

        List<String> tenantNames = tenants.stream()
                .map(Tenant::getName)
                .collect(Collectors.toList());

        int totalNotified = 0;
        int totalErrors = 0;

        for (String tenantName : tenantNames) {
            try {
                NotificationResult result = notifyExpiringKeysForTenant(tenantName);
                totalNotified += result.notifiedCount;
                totalErrors += result.errorCount;
            } catch (Exception e) {
                log.error("Error processing tenant: {}", tenantName, e);
                totalErrors++;
            }
        }

        log.info("Key expiry notification completed. Notifications sent: {}, Errors: {}", totalNotified, totalErrors);
    }

    private NotificationResult notifyExpiringKeysForTenant(String tenant) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.plusDays(warningDays);

        // Find enabled keys whose expiration date is between now and now+warningDays
        List<KmsKey> expiringKeys = kmsKeyRepository.findByTenantAndExpirationModelAndValidToBefore(
                tenant,
                IEnumKeyExpirationModel.Types.KEY_MATERIAL_EXPIRES,
                now
        );

        if (CollectionUtils.isEmpty(expiringKeys)) {
            log.debug("No expiring keys found for tenant: {}", tenant);
            return new NotificationResult(0, 0);
        }

        int notifiedCount = 0;
        int errorCount = 0;

        for (KmsKey key : expiringKeys) {
            try {
                log.info("Key will expire soon: {} (tenant: {}, expiration date: {})",
                        key.getKeyId(), tenant, key.getValidTo());

                sendNotification(tenant, key);
                notifiedCount++;

            } catch (Exception e) {
                log.error("Failed to send expiry notification for key: {} (tenant: {})", key.getKeyId(), tenant, e);
                errorCount++;
            }
        }

        if (notifiedCount > 0 || errorCount > 0) {
            log.info("Tenant {}: notified={}, errors={}", tenant, notifiedCount, errorCount);
        }
        return new NotificationResult(notifiedCount, errorCount);
    }

    private void sendNotification(String tenant, KmsKey key) {
        /*if (notificationService != null) {
            // Example: send an email to the tenant admin
            notificationService.sendKeyExpiryWarning(tenant, key.getKeyId(), key.getExpirationDate());
        } else {
            // Fallback: just log a warning (could also write to a dedicated alert table)
            log.warn("No notification service configured. Key {} for tenant {} expires on {}",
                    key.getKeyId(), tenant, key.getExpirationDate());
        }*/
    }

    // Helper class to hold notification results
    private static class NotificationResult {
        final int notifiedCount;
        final int errorCount;

        NotificationResult(int notifiedCount, int errorCount) {
            this.notifiedCount = notifiedCount;
            this.errorCount = errorCount;
        }
    }
}