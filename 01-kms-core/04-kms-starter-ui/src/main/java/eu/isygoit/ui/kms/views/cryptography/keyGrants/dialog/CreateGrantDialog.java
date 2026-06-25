package eu.isygoit.ui.kms.views.cryptography.keyGrants.dialog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
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
        super(I18n.t("grant.dialog.create.title"), onSuccess);
        this.keyId = keyId;
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        setOkButtonText(I18n.t("grant.dialog.create.button"));
        setWidth("500px");
        buildForm();
    }

    private void buildForm() {
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setWidthFull();

        granteeField = new TextField(I18n.t("grant.dialog.field.grantee"));
        granteeField.setRequired(true);
        granteeField.setPlaceholder(I18n.t("grant.dialog.field.grantee.placeholder"));
        granteeField.setWidthFull();

        retiringField = new TextField(I18n.t("grant.dialog.field.retiring"));
        retiringField.setPlaceholder(I18n.t("grant.dialog.field.retiring.placeholder"));
        retiringField.setWidthFull();

        operationsGroup = new CheckboxGroup<>(I18n.t("grant.dialog.field.operations"));
        operationsGroup.setItems(
                I18n.t("grant.dialog.operation.decrypt"),
                I18n.t("grant.dialog.operation.encrypt"),
                I18n.t("grant.dialog.operation.generate.data.key"),
                I18n.t("grant.dialog.operation.generate.data.key.without.plaintext"),
                I18n.t("grant.dialog.operation.reencrypt.from"),
                I18n.t("grant.dialog.operation.reencrypt.to"),
                I18n.t("grant.dialog.operation.sign"),
                I18n.t("grant.dialog.operation.verify"),
                I18n.t("grant.dialog.operation.generate.mac"),
                I18n.t("grant.dialog.operation.verify.mac"),
                I18n.t("grant.dialog.operation.get.public.key"),
                I18n.t("grant.dialog.operation.describe.key"),
                I18n.t("grant.dialog.operation.retire.grant")
        );
        operationsGroup.setRequired(true);
        operationsGroup.setWidthFull();

        constraintsArea = new TextArea(I18n.t("grant.dialog.field.constraints"));
        constraintsArea.setPlaceholder(I18n.t("grant.dialog.field.constraints.placeholder"));
        constraintsArea.setHeight("100px");

        nameField = new TextField(I18n.t("grant.dialog.field.name"));
        nameField.setPlaceholder(I18n.t("grant.dialog.field.name.placeholder"));
        nameField.setWidthFull();

        content.add(granteeField, retiringField, operationsGroup, constraintsArea, nameField);
        add(content);
    }

    @Override
    protected boolean onOk() {
        String grantee = granteeField.getValue();
        if (!StringUtils.hasText(grantee)) {
            append(I18n.t("grant.dialog.field.grantee.required"));
            return false;
        }
        if (!grantee.matches("^wrn:wams:iam::\\d+:.*")) {
            append(I18n.t("grant.dialog.field.grantee.invalid"));
            return false;
        }

        String retiring = retiringField.getValue();
        if (StringUtils.hasText(retiring) && !retiring.matches("^wrn:wams:iam::\\d+:.*")) {
            append(I18n.t("grant.dialog.field.retiring.invalid"));
            return false;
        }

        if (operationsGroup.getSelectedItems().isEmpty()) {
            append(I18n.t("grant.dialog.field.operations.required"));
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
                append(I18n.t("grant.dialog.field.constraints.invalid", e.getMessage()));
                return false;
            }
        }

        String name = nameField.getValue();
        if (StringUtils.hasText(name) && name.length() > 128) {
            append(I18n.t("grant.dialog.field.name.too.long"));
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
                append(I18n.t("grant.dialog.create.success"));
                return true;
            } else {
                append(I18n.t("grant.dialog.create.failed", response.getStatusCode()));
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("grant.dialog.create.failed", e.getMessage()));
        }

        return false;
    }
}