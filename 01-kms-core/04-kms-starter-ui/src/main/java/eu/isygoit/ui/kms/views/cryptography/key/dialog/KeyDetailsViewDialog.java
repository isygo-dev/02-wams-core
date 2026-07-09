package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.GetKeyPolicyResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Dialog displaying detailed information about a KMS key, including metadata, tags, and policy.
 *
 * <p>Fields are grouped into sections strictly by data type/purpose, mirroring the
 * {@code *DetailsViewDialog} convention used elsewhere in this module (see
 * {@code RandomKeyDetailsViewDialog}, {@code AliasDetailsViewDialog}, {@code CustomKeyStoreDetailsViewDialog}):
 * <ul>
 *   <li>Identity — free-text identifying fields (key id, WRN, tenant, alias, description, key manager)</li>
 *   <li>Classification &amp; status — enums/booleans describing what the key is and its current state</li>
 *   <li>Cryptographic configuration — rotation, current version, algorithm lists, multi-region config</li>
 *   <li>Dates — every timestamp field (creation, update, expiry, pending-deletion window/date)</li>
 *   <li>Tags / Policy — unchanged free-form sections at the end</li>
 * </ul>
 */
@Slf4j
public class KeyDetailsViewDialog extends DetailsViewDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    private final String keyId;
    private final DescribeKeyResponse.KeyMetadata metadata;

    public KeyDetailsViewDialog(KeyManagementView parentView,
                             KmsApiService kmsApiService,
                             ObjectMapper objectMapper,
                             String keyId,
                             DescribeKeyResponse.KeyMetadata metadata) {
        super(I18n.t("kms.key.dialog.describe.title"));
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.metadata = metadata;

        setWidth("750px");
        setMaxWidth("95%");
        setResizable(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);
        addClassName("describe-key-dialog");

        buildContent();
    }

    private void buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);
        content.setWidthFull();

        content.add(createSection(I18n.t("kms.key.dialog.describe.section.identity"), buildIdentityGrid()));
        content.add(createSection(I18n.t("kms.key.dialog.describe.section.classification"), buildClassificationGrid()));
        content.add(createSection(I18n.t("kms.key.dialog.describe.section.crypto"), buildCryptoGrid()));
        content.add(createSection(I18n.t("kms.key.dialog.describe.section.dates"), buildDatesGrid()));

        // Tags (as chips)
        List<ListResourceTagsResponse.Tag> tags = fetchTags();
        if (!tags.isEmpty()) {
            Div tagsContainer = new Div();
            tagsContainer.addClassName("tags-container");
            Span label = new Span(I18n.t("kms.key.dialog.describe.field.tags"));
            label.addClassName(LumoUtility.FontWeight.BOLD);
            tagsContainer.add(label);
            for (ListResourceTagsResponse.Tag tag : tags) {
                Span chip = new Span(I18n.t("kms.key.dialog.describe.tags.chip.format", tag.getTagKey(), tag.getTagValue()));
                chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                chip.addClassName(LumoUtility.BorderRadius.LARGE);
                chip.addClassName("wams-chip--info");
                chip.addClassName("tag-chip");
                tagsContainer.add(chip);
            }
            content.add(createSection(I18n.t("kms.key.dialog.describe.section.tags"), tagsContainer));
        }

        // Policy (pretty JSON)
        try {
            ResponseEntity<GetKeyPolicyResponse> policyResponse = kmsApiService.getKeyPolicy(keyId);
            if (policyResponse.getStatusCode().is2xxSuccessful() && policyResponse.getBody() != null) {
                Object policyObj = policyResponse.getBody().getPolicy();
                String prettyPolicy = null;
                if (policyObj instanceof String) {
                    Object json = objectMapper.readValue((String) policyObj, Object.class);
                    prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                } else if (policyObj instanceof Map) {
                    prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyObj);
                }
                if (prettyPolicy != null && !prettyPolicy.isBlank()) {
                    TextArea policyArea = new TextArea();
                    policyArea.setValue(prettyPolicy);
                    policyArea.setWidthFull();
                    policyArea.setHeight("300px");
                    policyArea.setReadOnly(true);
                    policyArea.addClassName("policy-textarea");
                    content.add(createSection(I18n.t("kms.key.dialog.describe.field.policy"), policyArea));
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch or format policy for key {}", keyId, e);
        }

        add(content);
    }

    // ─── Section builders (strictly grouped by data type) ───────────────────

    /** Identity — free-text identifying fields. */
    private Component buildIdentityGrid() {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.KEY, I18n.t("kms.key.dialog.describe.field.key.id"), metadata.getKeyId(), true);
        addFieldToGrid(grid, VaadinIcon.HASH, I18n.t("kms.key.dialog.describe.field.wrn"), metadata.getWrn(), true);
        addFieldToGrid(grid, VaadinIcon.BUILDING, I18n.t("kms.key.dialog.describe.field.tenant"), metadata.getTenant());
        addFieldToGrid(grid, VaadinIcon.TAG, I18n.t("kms.key.dialog.describe.field.alias"), metadata.getKeyAlias(), true);
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("kms.key.dialog.describe.field.description"), metadata.getDescription());
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("kms.key.dialog.describe.field.key.manager"), metadata.getKeyManager());
        return grid;
    }

    /** Classification &amp; status — enums/booleans describing what the key is and its current state. */
    private Component buildClassificationGrid() {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FLAG, I18n.t("kms.key.dialog.describe.field.status"),
                metadata.getKeyStatus() != null ? metadata.getKeyStatus().name() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.CHECK_CIRCLE, I18n.t("kms.key.dialog.describe.field.enabled"),
                metadata.getEnabled() != null ? metadata.getEnabled().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.COG, I18n.t("kms.key.dialog.describe.field.key.spec"),
                metadata.getKeySpec() != null ? metadata.getKeySpec().name() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.COGS, I18n.t("kms.key.dialog.describe.field.key.usage"),
                metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.COG_O, I18n.t("kms.key.dialog.describe.field.customer.master.key.spec"), metadata.getCustomerMasterKeySpec());
        addFieldToGrid(grid, VaadinIcon.CLOUD, I18n.t("kms.key.dialog.describe.field.origin"),
                metadata.getOrigin() != null ? metadata.getOrigin().name() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.HOURGLASS, I18n.t("kms.key.dialog.describe.field.expiration.model"),
                metadata.getExpirationModel() != null ? metadata.getExpirationModel().name() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.GLOBE, I18n.t("kms.key.dialog.describe.field.multi.region"),
                metadata.getMultiRegion() != null ? metadata.getMultiRegion().toString() : Boolean.FALSE.toString());
        return grid;
    }

    /** Cryptographic configuration — rotation, current version, algorithm lists, multi-region config. */
    private Component buildCryptoGrid() {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.REFRESH, I18n.t("kms.key.dialog.describe.field.rotation.enabled"),
                metadata.getRotationEnabled() != null ? metadata.getRotationEnabled().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.CLOCK, I18n.t("kms.key.dialog.describe.field.rotation.period"),
                metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.CUBE, I18n.t("kms.key.dialog.describe.field.current.version"), metadata.getCurrentVersion(), true);
        addFieldToGrid(grid, VaadinIcon.LOCK, I18n.t("kms.key.dialog.describe.field.encryption.algorithms"),
                (metadata.getEncryptionAlgorithmSpecs() != null && !metadata.getEncryptionAlgorithmSpecs().isEmpty())
                        ? String.join(", ", metadata.getEncryptionAlgorithmSpecs()) : null);
        addFieldToGrid(grid, VaadinIcon.SIGN_IN_ALT, I18n.t("kms.key.dialog.describe.field.signing.algorithms"),
                (metadata.getSigningAlgorithms() != null && !metadata.getSigningAlgorithms().isEmpty())
                        ? String.join(", ", metadata.getSigningAlgorithms()) : null);

        if (metadata.getMultiRegionConfiguration() != null) {
            try {
                String mrConfig = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(metadata.getMultiRegionConfiguration());
                addFieldToGrid(grid, VaadinIcon.CLUSTER, I18n.t("kms.key.dialog.describe.field.multi.region.config"), mrConfig);
            } catch (Exception e) {
                addFieldToGrid(grid, VaadinIcon.CLUSTER, I18n.t("kms.key.dialog.describe.field.multi.region.config"),
                        metadata.getMultiRegionConfiguration().toString());
            }
        }
        return grid;
    }

    /** Dates — every timestamp / date-window field. */
    private Component buildDatesGrid() {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.CALENDAR, I18n.t("kms.key.dialog.describe.field.creation.date"),
                metadata.getCreateDate() != null ? metadata.getCreateDate().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.CALENDAR_O, I18n.t("kms.key.dialog.describe.field.updated.date"),
                metadata.getUpdateDate() != null ? metadata.getUpdateDate().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        addFieldToGrid(grid, VaadinIcon.CALENDAR_CLOCK, I18n.t("kms.key.dialog.describe.field.valid.until"),
                metadata.getValidTo() != null ? metadata.getValidTo().toString() : I18n.t("kms.key.dialog.describe.placeholder"));

        if (metadata.getPendingDeletionWindowDays() != null || metadata.getDeletionDate() != null) {
            addFieldToGrid(grid, VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.key.dialog.describe.field.pending.deletion.window"),
                    metadata.getPendingDeletionWindowDays() != null ? metadata.getPendingDeletionWindowDays().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
            addFieldToGrid(grid, VaadinIcon.TRASH, I18n.t("kms.key.dialog.describe.field.deletion.date"),
                    metadata.getDeletionDate() != null ? metadata.getDeletionDate().toString() : I18n.t("kms.key.dialog.describe.placeholder"));
        }
        return grid;
    }

    private List<ListResourceTagsResponse.Tag> fetchTags() {
        try {
            ResponseEntity<ListResourceTagsResponse> response = kmsApiService.listResourceTags(keyId, 100, null);
            ListResourceTagsResponse tagsResponse = response.getBody();
            if (tagsResponse != null && tagsResponse.getTags() != null) {
                return tagsResponse.getTags();
            }
        } catch (Exception e) {
            log.error("Failed to load tags for key {}", keyId, e);
        }
        return List.of();
    }
}
