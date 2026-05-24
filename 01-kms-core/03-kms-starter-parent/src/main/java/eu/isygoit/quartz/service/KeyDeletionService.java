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
 * Service responsible for automatic deletion of keys that are in PENDING_DELETION state
 * and whose scheduled deletion date has passed.
 * This job is scheduled to run every hour via Quartz.
 */
@Slf4j
@Service
public class KeyDeletionService extends AbstractJobService {

    @Autowired
    private KmsKeyRepository kmsKeyRepository;

    @Autowired
    private IKeyManagementService keyManagementService;

    @Autowired
    private TenantRepository tenantRepository;

    @Override
    public void performJob(JobExecutionContext jobExecutionContext) {
        log.info("Starting automatic key deletion check...");

        // Fetch all tenants
        List<Tenant> tenants = tenantRepository.findAll();
        if (CollectionUtils.isEmpty(tenants)) {
            log.warn("No tenants found, skipping key deletion check");
            return;
        }

        List<String> tenantNames = tenants.stream()
                .map(Tenant::getName)
                .collect(Collectors.toList());

        int totalDeleted = 0;
        int totalErrors = 0;

        for (String tenantName : tenantNames) {
            try {
                DeletionResult result = deleteKeysForTenant(tenantName);
                totalDeleted += result.deletedCount;
                totalErrors += result.errorCount;
            } catch (Exception e) {
                log.error("Error processing tenant: {}", tenantName, e);
                totalErrors++;
            }
        }

        log.info("Automatic key deletion completed. Total deleted: {}, Errors: {}", totalDeleted, totalErrors);
    }

    private DeletionResult deleteKeysForTenant(String tenant) {
        // Find all keys in PENDING_DELETION state whose scheduled deletion date has passed
        LocalDateTime now = LocalDateTime.now();
        List<KmsKey> keysToDelete = kmsKeyRepository.findByTenantAndKeyStatusAndDeletionDateBefore(
                tenant, IEnumKeyStatus.Types.PENDING_DELETION, now);

        if (CollectionUtils.isEmpty(keysToDelete)) {
            log.debug("No keys pending deletion found for tenant: {}", tenant);
            return new DeletionResult(0, 0);
        }

        int deletedCount = 0;
        int errorCount = 0;

        for (KmsKey key : keysToDelete) {
            try {
                log.info("Permanently deleting key: {} (tenant: {}, scheduled deletion date: {})",
                        key.getKeyId(), tenant, key.getDeletionDate());

                // Call KMS API to delete the key and all its versions
                keyManagementService.deleteKey(tenant, key.getKeyId());

                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete key: {} (tenant: {})", key.getKeyId(), tenant, e);
                errorCount++;
            }
        }

        if (deletedCount > 0 || errorCount > 0) {
            log.info("Tenant {}: deleted={}, errors={}", tenant, deletedCount, errorCount);
        }
        return new DeletionResult(deletedCount, errorCount);
    }

    // Helper class to hold deletion results
    private static class DeletionResult {
        final int deletedCount;
        final int errorCount;

        DeletionResult(int deletedCount, int errorCount) {
            this.deletedCount = deletedCount;
            this.errorCount = errorCount;
        }
    }
}