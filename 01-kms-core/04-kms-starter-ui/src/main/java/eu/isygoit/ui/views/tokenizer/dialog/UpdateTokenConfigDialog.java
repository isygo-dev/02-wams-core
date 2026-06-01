package eu.isygoit.ui.views.tokenizer.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.TokenConfigDto;
import eu.isygoit.enums.IEnumToken;
import eu.isygoit.remote.kms.KmsTokenConfigService;
import eu.isygoit.ui.views.BaseActionDialog;
import org.springframework.http.ResponseEntity;

public class UpdateTokenConfigDialog extends BaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;
    private final TokenConfigDto original;

    private TextField codeField;
    private ComboBox<IEnumToken.Types> tokenTypeCombo;
    private TextField issuerField;
    private TextField audienceField;
    private TextField signatureAlgorithmField;
    private TextField secretKeyField;

    public UpdateTokenConfigDialog(KmsTokenConfigService tokenConfigService, TokenConfigDto dto, Runnable onSuccess) {
        super("Edit Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;
        this.original = dto;

        setOkButtonText("Save");
        setWidth("600px");

        buildForm();
        bindData();
        add(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setReadOnly(true);  // code cannot be changed after creation

        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);

        issuerField = new TextField("Issuer");
        audienceField = new TextField("Audience");
        signatureAlgorithmField = new TextField("Signature algorithm");
        secretKeyField = new TextField("Secret key");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setHelperText("Minimum 32 bytes (256 bits) for HS256");
    }

    private void bindData() {
        codeField.setValue(original.getCode());
        tokenTypeCombo.setValue(original.getTokenType());
        issuerField.setValue(original.getIssuer() != null ? original.getIssuer() : "");
        audienceField.setValue(original.getAudience() != null ? original.getAudience() : "");
        signatureAlgorithmField.setValue(original.getSignatureAlgorithm() != null ? original.getSignatureAlgorithm() : "");
        secretKeyField.setValue(original.getSecretKey() != null ? original.getSecretKey() : "");
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.add(codeField, tokenTypeCombo, issuerField, audienceField, signatureAlgorithmField, secretKeyField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        form.setColspan(codeField, 1);
        form.setColspan(tokenTypeCombo, 1);
        form.setColspan(issuerField, 2);
        form.setColspan(audienceField, 2);
        form.setColspan(signatureAlgorithmField, 1);
        form.setColspan(secretKeyField, 2);
        return form;
    }

    @Override
    protected boolean onOk() {
        IEnumToken.Types tokenType = tokenTypeCombo.getValue();
        if (tokenType == null) {
            showError("Token type is required");
            return false;
        }
        String secretKey = secretKeyField.getValue();
        if (secretKey == null || secretKey.isBlank()) {
            showError("Secret key is required");
            return false;
        }

        TokenConfigDto updated = TokenConfigDto.builder()
                .id(original.getId())
                .code(original.getCode()) // code is read‑only
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(audienceField.getValue())
                .signatureAlgorithm(signatureAlgorithmField.getValue())
                .secretKey(secretKey)
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.update(original.getId(), updated);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration updated successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                String errorMsg = "Update failed: " + response.getStatusCode();
                showError(errorMsg);
                return false;
            }
        } catch (Exception e) {
            String errorMsg = "Error: " + e.getMessage();
            showError(errorMsg);
            return false;
        }
    }
}