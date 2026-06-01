package eu.isygoit.ui.views.key.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.DescribeKeyResponse;
import eu.isygoit.dto.KmsDtos.GetKeyPolicyResponse;
import eu.isygoit.dto.KmsDtos.ListResourceTagsResponse;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.NoActionDialog;
import eu.isygoit.ui.views.key.KeyManagementView;
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
        super("Key details");
        this.parentView = parentView;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        this.keyId = keyId;
        this.metadata = metadata;

        setWidth("750px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        buildContent();
        addFooter();
    }

    private void buildContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        // Basic metadata
        content.add(detailRow("Key ID", metadata.getKeyId()),
                detailRow("WRN", metadata.getWrn()),
                detailRow("Alias", metadata.getKeyAlias()),
                detailRow("Description", metadata.getDescription()),
                detailRow("Status", metadata.getKeyStatus() != null ? metadata.getKeyStatus().name() : "N/A"),
                detailRow("Key spec", metadata.getKeySpec() != null ? metadata.getKeySpec().name() : "N/A"),
                detailRow("Key usage", metadata.getKeyUsage() != null ? metadata.getKeyUsage().name() : "N/A"),
                detailRow("Customer master key spec", metadata.getCustomerMasterKeySpec()),
                detailRow("Origin", metadata.getOrigin() != null ? metadata.getOrigin().name() : "N/A"),
                detailRow("Creation date", metadata.getCreateDate() != null ? metadata.getCreateDate().toString() : "N/A"),
                detailRow("Rotation enabled", metadata.getRotationEnabled() != null ? metadata.getRotationEnabled().toString() : "N/A"),
                detailRow("Rotation period (days)", metadata.getRotationPeriodInDays() != null ? metadata.getRotationPeriodInDays().toString() : "N/A"),
                detailRow("Current version", metadata.getCurrentVersion()),
                detailRow("Key manager", metadata.getKeyManager()),
                detailRow("Expiration model", metadata.getExpirationModel() != null ? metadata.getExpirationModel().name() : "N/A"),
                detailRow("Valid until", metadata.getValidTo() != null ? metadata.getValidTo().toString() : "N/A"),
                detailRow("Multi-region", metadata.getMultiRegion() != null ? metadata.getMultiRegion().toString() : "false"));

        // Multi‑region configuration (if present)
        if (metadata.getMultiRegionConfiguration() != null) {
            try {
                String mrConfig = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(metadata.getMultiRegionConfiguration());
                if (mrConfig != null && !mrConfig.isBlank()) {
                    content.add(detailRow("Multi-region config", mrConfig));
                }
            } catch (Exception e) {
                content.add(detailRow("Multi-region config", metadata.getMultiRegionConfiguration().toString()));
            }
        }

        // Algorithm lists
        if (metadata.getEncryptionAlgorithmSpecs() != null && !metadata.getEncryptionAlgorithmSpecs().isEmpty()) {
            content.add(detailRow("Encryption algorithms", String.join(", ", metadata.getEncryptionAlgorithmSpecs())));
        }
        if (metadata.getSigningAlgorithms() != null && !metadata.getSigningAlgorithms().isEmpty()) {
            content.add(detailRow("Signing algorithms", String.join(", ", metadata.getSigningAlgorithms())));
        }

        // Tags (as chips)
        List<ListResourceTagsResponse.Tag> tags = fetchTags();
        if (!tags.isEmpty()) {
            Div tagsContainer = new Div();
            tagsContainer.getStyle()
                    .set("display", "flex")
                    .set("flex-wrap", "wrap")
                    .set("gap", "var(--lumo-space-xs)")
                    .set("margin-top", "var(--lumo-space-s)");
            Span label = new Span("Tags: ");
            label.addClassName(LumoUtility.FontWeight.BOLD);
            tagsContainer.add(label);
            for (ListResourceTagsResponse.Tag tag : tags) {
                Span chip = new Span(tag.getTagKey() + "=" + tag.getTagValue());
                chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                chip.addClassName(LumoUtility.BorderRadius.LARGE);
                chip.getStyle()
                        .set("background-color", "#E9ECEF")
                        .set("color", "#495057")
                        .set("white-space", "nowrap");
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
                    Span label = new Span("Policy: ");
                    label.addClassName(LumoUtility.FontWeight.BOLD);
                    content.add(label);
                    TextArea policyArea = new TextArea();
                    policyArea.setValue(prettyPolicy);
                    policyArea.setWidthFull();
                    policyArea.setHeight("300px");
                    policyArea.setReadOnly(true);
                    policyArea.getStyle().set("font-family", "monospace");
                    content.add(policyArea);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch or format policy for key {}", keyId, e);
        }

        add(content);
    }

    private void addFooter() {
        Button closeBtn = new Button("Close", e -> close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(closeBtn);
    }

    private HorizontalLayout detailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);
        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.BOLD);
        labelSpan.setWidth("30%");
        Span valueSpan = new Span(value != null ? value : "-");
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