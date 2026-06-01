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

public class CreateTokenConfigDialog extends BaseActionDialog {

    private final KmsTokenConfigService tokenConfigService;

    private TextField codeField;
    private ComboBox<IEnumToken.Types> tokenTypeCombo;
    private TextField issuerField;
    private TextField audienceField;
    private TextField signatureAlgorithmField;
    private TextField secretKeyField;

    public CreateTokenConfigDialog(KmsTokenConfigService tokenConfigService, Runnable onSuccess) {
        super("Create Token Configuration", onSuccess);
        this.tokenConfigService = tokenConfigService;

        setOkButtonText("Create");
        setWidth("600px");

        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder("e.g., KMS_ACCESS_TOKEN");

        tokenTypeCombo = new ComboBox<>("Token type");
        tokenTypeCombo.setItems(IEnumToken.Types.values());
        tokenTypeCombo.setRequired(true);
        tokenTypeCombo.setRequiredIndicatorVisible(true);
        tokenTypeCombo.setValue(IEnumToken.Types.ACCESS);

        issuerField = new TextField("Issuer");
        issuerField.setPlaceholder("e.g., https://kms.isygoit.eu");

        audienceField = new TextField("Audience");
        audienceField.setPlaceholder("e.g., kms-console");

        signatureAlgorithmField = new TextField("Signature algorithm");
        signatureAlgorithmField.setPlaceholder("e.g., HS256, RS256");
        signatureAlgorithmField.setValue("HS256");

        secretKeyField = new TextField("Secret key");
        secretKeyField.setRequired(true);
        secretKeyField.setRequiredIndicatorVisible(true);
        secretKeyField.setHelperText("Minimum 32 bytes (256 bits) for HS256");
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
        String code = codeField.getValue();
        if (code == null || code.isBlank()) {
            showError("Code is required");
            return false;
        }
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

        TokenConfigDto dto = TokenConfigDto.builder()
                .code(code)
                .tokenType(tokenType)
                .issuer(issuerField.getValue())
                .audience(audienceField.getValue())
                .signatureAlgorithm(signatureAlgorithmField.getValue())
                .secretKey(secretKey)
                .build();

        try {
            ResponseEntity<TokenConfigDto> response = tokenConfigService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration created successfully", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                String errorMsg = "Creation failed: " + response.getStatusCode();
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