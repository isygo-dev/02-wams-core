package eu.isygoit.quartz.service;

import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.model.KmsKey;
import eu.isygoit.model.Tenant;
import eu.isygoit.repository.KmsKeyRepository;
import eu.isygoit.repository.TenantRepository;
import eu.isygoit.service.IKeyManagementService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for automatic key rotation based on configured rotation policies.
 * This job is scheduled to run every 4 hours via Quartz.
 */
@Slf4j
@Service
public class KeyRotationService extends AbstractJobService {

    @Autowired
    private KmsKeyRepository kmsKeyRepository;

    @Autowired
    private IKeyManagementService keyManagementService;

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {
        log.info("Starting automatic key rotation check...");

        // Fetch all tenants from the database
        List<Tenant> tenants = tenantRepository.findAll();
        if (CollectionUtils.isEmpty(tenants)) {
            log.warn("No tenants found, skipping key rotation check");
            return;
        }

        List<String> tenantNames = tenants.stream()
                .map(Tenant::getName) // assuming Tenant has a getName() method
                .collect(Collectors.toList());

        int totalRotated = 0;
        int totalSkippedReplicas = 0;
        int totalErrors = 0;

        for (String tenantName : tenantNames) {
            try {
                RotationResult result = rotateKeysForTenant(tenantName);
                totalRotated += result.rotatedCount;
                totalSkippedReplicas += result.skippedReplicaCount;
                totalErrors += result.errorCount;
            } catch (Exception e) {
                log.error("Error processing tenant: {}", tenantName, e);
                totalErrors++;
            }
        }

        log.info("Automatic key rotation completed. Total rotated: {}, Skipped replicas: {}, Errors: {}",
                totalRotated, totalSkippedReplicas, totalErrors);
    }

    private RotationResult rotateKeysForTenant(String tenant) {
        // Fetch all enabled keys with rotation enabled for this tenant
        List<KmsKey> keys = kmsKeyRepository.findByTenantAndKeyStatusAndRotationEnabled(
                tenant, IEnumKeyStatus.Types.ENABLED, true);

        if (CollectionUtils.isEmpty(keys)) {
            log.debug("No rotation-enabled keys found for tenant: {}", tenant);
            return new RotationResult(0, 0, 0);
        }

        LocalDateTime now = LocalDateTime.now();
        int rotatedCount = 0;
        int skippedReplicaCount = 0;
        int errorCount = 0;

        for (KmsKey key : keys) {
            // Skip replica keys – only primary keys should be rotated
            if (key.isReplicaKey()) {
                log.debug("Skipping replica key: {} (tenant: {})", key.getKeyId(), tenant);
                skippedReplicaCount++;
                continue;
            }

            // Determine base date for rotation
            LocalDateTime baseDate = key.getLastRotationDate() != null
                    ? key.getLastRotationDate()
                    : key.getCreateDate();

            if (baseDate == null) {
                log.warn("Key {} has no creation date or last rotation date, skipping", key.getKeyId());
                skippedReplicaCount++;
                continue;
            }

            // Calculate next rotation due date
            //LocalDateTime dueDate = baseDate.plusDays(key.getRotationPeriodInDays());
            LocalDateTime dueDate = baseDate.plusMinutes(5);
            if (dueDate.isBefore(now) || dueDate.isEqual(now)) {
                try {
                    log.info("Rotating key: {} (tenant: {}, last rotation: {}, period: {} days)",
                            key.getKeyId(), tenant, key.getLastRotationDate(), key.getRotationPeriodInDays());
                    keyManagementService.rotateKey(tenant, key.getKeyId());
                    rotatedCount++;
                } catch (Exception e) {
                    log.error("Failed to rotate key: {} (tenant: {})", key.getKeyId(), tenant, e);
                    errorCount++;
                }
            }
        }

        if (rotatedCount > 0 || skippedReplicaCount > 0 || errorCount > 0) {
            log.info("Tenant {}: rotated={}, skippedReplicas={}, errors={}",
                    tenant, rotatedCount, skippedReplicaCount, errorCount);
        }
        return new RotationResult(rotatedCount, skippedReplicaCount, errorCount);
    }

    // Helper class to hold rotation results
    private static class RotationResult {
        final int rotatedCount;
        final int skippedReplicaCount;
        final int errorCount;

        RotationResult(int rotatedCount, int skippedReplicaCount, int errorCount) {
            this.rotatedCount = rotatedCount;
            this.skippedReplicaCount = skippedReplicaCount;
            this.errorCount = errorCount;
        }
    }
}