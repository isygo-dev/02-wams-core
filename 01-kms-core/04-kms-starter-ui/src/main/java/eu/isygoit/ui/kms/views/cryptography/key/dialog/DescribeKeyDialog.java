package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.GetKeyPolicyResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.kms.views.cryptography.key.KeyManagementView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * Dialog displaying detailed information about a KMS key, including metadata, tags, and policy.
 */
@Slf4j
public class DescribeKeyDialog extends NoActionDialog {

    private final KeyManagementView parentView;
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;
    private final String keyId;
    private final DescribeKeyResponse.KeyMetadata metadata;

    public DescribeKeyDialog(KeyManagementView parentView,
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

        // Basic metadata
        content.add(detailRow(I18n.t("kms.key.dialog.describe.field.key.id"), metadata.getKeyId()),
                detailRow(I18n.t("kms.key.dialog.describe.field.wrn"), metadata.getWrn()),
                detailRow(I18n.t("kms.key.dialog.describe.field.alias"), metadata.getKeyAlias()),
                detailRow(I18n.t("kms.key.dialog.describe.field.description"), metadata.getDescription()),
                detailRow(I18n.t("kms.key.dialog.describe.field.status"), metadata.getKeyStatus() != null ? metadata.getKeyStatus().name() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.key.spec"), metadata.getKeySpec() != null ? metadata.getKeySpec().name() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.key.usage"), metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.customer.master.key.spec"), metadata.getCustomerMasterKeySpec()),
                detailRow(I18n.t("kms.key.dialog.describe.field.origin"), metadata.getOrigin() != null ? metadata.getOrigin().name() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.creation.date"), metadata.getCreateDate() != null ? metadata.getCreateDate().toString() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.rotation.enabled"), metadata.getRotationEnabled() != null ? metadata.getRotationEnabled().toString() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.rotation.period"), metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays().toString() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.current.version"), metadata.getCurrentVersion()),
                detailRow(I18n.t("kms.key.dialog.describe.field.key.manager"), metadata.getKeyManager()),
                detailRow(I18n.t("kms.key.dialog.describe.field.expiration.model"), metadata.getExpirationModel() != null ? metadata.getExpirationModel().name() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.valid.until"), metadata.getValidTo() != null ? metadata.getValidTo().toString() : I18n.t("kms.key.dialog.describe.placeholder")),
                detailRow(I18n.t("kms.key.dialog.describe.field.multi.region"), metadata.getMultiRegion() != null ? metadata.getMultiRegion().toString() : Boolean.FALSE.toString()));

        // Multi‑region configuration (if present)
        if (metadata.getMultiRegionConfiguration() != null) {
            try {
                String mrConfig = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(metadata.getMultiRegionConfiguration());
                if (mrConfig != null && !mrConfig.isBlank()) {
                    content.add(detailRow(I18n.t("kms.key.dialog.describe.field.multi.region.config"), mrConfig));
                }
            } catch (Exception e) {
                content.add(detailRow(I18n.t("kms.key.dialog.describe.field.multi.region.config"), metadata.getMultiRegionConfiguration().toString()));
            }
        }

        // Algorithm lists
        if (metadata.getEncryptionAlgorithmSpecs() != null && !metadata.getEncryptionAlgorithmSpecs().isEmpty()) {
            content.add(detailRow(I18n.t("kms.key.dialog.describe.field.encryption.algorithms"), String.join(", ", metadata.getEncryptionAlgorithmSpecs())));
        }
        if (metadata.getSigningAlgorithms() != null && !metadata.getSigningAlgorithms().isEmpty()) {
            content.add(detailRow(I18n.t("kms.key.dialog.describe.field.signing.algorithms"), String.join(", ", metadata.getSigningAlgorithms())));
        }

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
            content.add(tagsContainer);
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
                    Span label = new Span(I18n.t("kms.key.dialog.describe.field.policy"));
                    label.addClassName(LumoUtility.FontWeight.BOLD);
                    content.add(label);
                    TextArea policyArea = new TextArea();
                    policyArea.setValue(prettyPolicy);
                    policyArea.setWidthFull();
                    policyArea.setHeight("300px");
                    policyArea.setReadOnly(true);
                    policyArea.addClassName("policy-textarea");
                    content.add(policyArea);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch or format policy for key {}", keyId, e);
        }

        add(content);
    }

    private HorizontalLayout detailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        row.addClassName("detail-row");
        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.BOLD);
        labelSpan.setWidth("30%");
        Span valueSpan = new Span(value != null ? value : I18n.t("kms.key.dialog.describe.placeholder"));
        valueSpan.setWidth("70%");
        row.add(labelSpan, valueSpan);
        return row;
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