package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateGrantDialog extends BaseActionDialog {

    private final String keyId;
    private final KmsApiService kmsApiService;
    private final ObjectMapper objectMapper;

    private TextField granteeField;
    private TextField retiringField;
    private CheckboxGroup<String> operationsGroup;
    private TextArea constraintsArea;
    private TextField nameField;

    public CreateGrantDialog(String keyId, KmsApiService kmsApiService, ObjectMapper objectMapper, Runnable onSuccess) {
        super("Create Grant", onSuccess);
        this.keyId = keyId;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        setOkButtonText("Create");
        setWidth("500px");
        buildForm();
    }

    private void buildForm() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();

        granteeField = new TextField("Grantee Principal");
        granteeField.setRequired(true);
        granteeField.setPlaceholder("wrn:wams:iam::123456789012:role/ExampleRole");
        granteeField.setWidthFull();

        retiringField = new TextField("Retiring Principal (optional)");
        retiringField.setPlaceholder("wrn:wams:iam::123456789012:user/Admin");
        retiringField.setWidthFull();

        operationsGroup = new CheckboxGroup<>("Operations");
        operationsGroup.setItems(
                "Decrypt", "Encrypt", "GenerateDataKey", "GenerateDataKeyWithoutPlaintext",
                "ReEncryptFrom", "ReEncryptTo", "Sign", "Verify", "GenerateMac", "VerifyMac",
                "GetPublicKey", "DescribeKey", "RetireGrant"
        );
        operationsGroup.setRequired(true);
        operationsGroup.setWidthFull();

        constraintsArea = new TextArea("Constraints (JSON)");
        constraintsArea.setPlaceholder("{\"encryptionContextSubset\": {\"key\":\"value\"}}");
        constraintsArea.setHeight("100px");

        nameField = new TextField("Name (optional)");
        nameField.setPlaceholder("Friendly name");
        nameField.setWidthFull();

        content.add(granteeField, retiringField, operationsGroup, constraintsArea, nameField);
        add(content);
    }

    @Override
    protected boolean onOk() {
        String grantee = granteeField.getValue();
        if (!StringUtils.hasText(grantee)) {
            append("Grantee principal is required");
            return false;
        }
        if (!grantee.matches("^wrn:wams:iam::\\d+:.*")) {
            append("Invalid Grantee Principal format. Expected: wrn:wams:iam::<account-id>:<type>/<name>");
            return false;
        }

        String retiring = retiringField.getValue();
        if (StringUtils.hasText(retiring) && !retiring.matches("^wrn:wams:iam::\\d+:.*")) {
            append("Invalid Retiring Principal format");
            return false;
        }

        if (operationsGroup.getSelectedItems().isEmpty()) {
            append("At least one operation must be selected");
            return false;
        }
        List<String> operations = new ArrayList<>(operationsGroup.getSelectedItems());

        String constraintsText = constraintsArea.getValue();
        KmsDtos.CreateGrantRequest.GrantConstraints constraints = null;
        if (StringUtils.hasText(constraintsText)) {
            try {
                Map<String, Object> constraintsMap = objectMapper.readValue(constraintsText, new TypeReference<>() {});
                constraints = KmsDtos.CreateGrantRequest.GrantConstraints.builder()
                        .encryptionContextSubset((Map<String, String>) constraintsMap.get("encryptionContextSubset"))
                        .encryptionContextEquals((Map<String, String>) constraintsMap.get("encryptionContextEquals"))
                        .build();
            } catch (Exception e) {
                append("Invalid constraints JSON: " + e.getMessage());
                return false;
            }
        }

        String name = nameField.getValue();
        if (StringUtils.hasText(name) && name.length() > 128) {
            append("Name too long (max 128 characters)");
            return false;
        }

        try {
            KmsDtos.CreateGrantRequest request = KmsDtos.CreateGrantRequest.builder()
                    .keyId(keyId)
                    .granteePrincipal(grantee)
                    .retiringPrincipal(StringUtils.hasText(retiring) ? retiring : null)
                    .operations(operations)
                    .constraints(constraints)
                    .name(StringUtils.hasText(name) ? name : null)
                    .build();
            ResponseEntity<KmsDtos.CreateGrantResponse> response = kmsApiService.createGrant(keyId, request);
            if (response.getStatusCode().is2xxSuccessful()) {
                append("Grant created successfully");
                return true;
            } else {
                append("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append("Failed operation: " + e.getMessage());
        }

        return false;
    }
}