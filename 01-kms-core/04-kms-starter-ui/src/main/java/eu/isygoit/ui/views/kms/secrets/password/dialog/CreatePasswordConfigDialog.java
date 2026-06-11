package eu.isygoit.ui.views.kms.secrets.password.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.views.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

public class CreatePasswordConfigDialog extends BaseActionDialog {

    private final PasswordConfigService configService;

    private TextField codeField;
    private ComboBox<IEnumAuth.Types> typeCombo;
    private TextField patternField;
    private ComboBox<IEnumCharSet.Types> charSetCombo;
    private TextField initialField;
    private IntegerField minLengthField;
    private IntegerField maxLengthField;
    private IntegerField lifeTimeField;

    public CreatePasswordConfigDialog(PasswordConfigService configService, Runnable onSuccess) {
        super("Create Password Configuration", onSuccess);
        this.configService = configService;
        setOkButtonText("Create");
        setWidth("600px");
        buildForm();
        add(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder("e.g., PWD_PROD");

        typeCombo = new ComboBox<>("Type");
        typeCombo.setItems(IEnumAuth.Types.values());
        typeCombo.setRequired(true);
        typeCombo.setRequiredIndicatorVisible(true);

        patternField = new TextField("Pattern (regex)");
        patternField.setPlaceholder("e.g., ^[A-Za-z0-9]+$");
        patternField.setWidthFull();

        charSetCombo = new ComboBox<>("Character set");
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setRequired(true);

        initialField = new TextField("Initial value");
        initialField.setPlaceholder("Optional starting value");

        minLengthField = new IntegerField("Min length");
        minLengthField.setValue(8);
        minLengthField.setMin(1);
        minLengthField.setRequired(true);

        maxLengthField = new IntegerField("Max length");
        maxLengthField.setValue(20);
        maxLengthField.setMin(1);
        maxLengthField.setRequired(true);

        lifeTimeField = new IntegerField("Lifetime (days)");
        lifeTimeField.setValue(90);
        lifeTimeField.setMin(1);
        lifeTimeField.setRequired(true);
    }

    private FormLayout createFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(codeField, typeCombo, patternField, charSetCombo, initialField,
                minLengthField, maxLengthField, lifeTimeField);
        return form;
    }

    @Override
    protected boolean onOk() {
        String code = codeField.getValue();
        if (!StringUtils.hasText(code)) {
            showError("Code is required");
            return false;
        }
        IEnumAuth.Types type = typeCombo.getValue();
        if (type == null) {
            showError("Type is required");
            return false;
        }
        IEnumCharSet.Types charSet = charSetCombo.getValue();
        if (charSet == null) {
            showError("Character set is required");
            return false;
        }
        Integer minLen = minLengthField.getValue();
        if (minLen == null || minLen < 1) {
            showError("Min length must be at least 1");
            return false;
        }
        Integer maxLen = maxLengthField.getValue();
        if (maxLen == null || maxLen < minLen) {
            showError("Max length must be >= min length");
            return false;
        }
        Integer lifeTime = lifeTimeField.getValue();
        if (lifeTime == null || lifeTime < 1) {
            showError("Lifetime must be at least 1 day");
            return false;
        }

        PasswordConfigDto dto = PasswordConfigDto.builder()
                .code(code)
                .type(type)
                .pattern(patternField.getValue())
                .charSetType(charSet)
                .initial(initialField.getValue())
                .minLength(minLen)
                .maxLength(maxLen)
                .lifeTime(lifeTime)
                .build();

        try {
            ResponseEntity<PasswordConfigDto> response = configService.create(dto);
            if (response.getStatusCode().is2xxSuccessful()) {
                Notification.show("Configuration created successfully", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                return true;
            } else {
                showError("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            handleFeignException(ex);
            return false;
        } catch (Exception ex) {
            handleGenericException(ex);
            return false;
        }
    }

    private void handleFeignException(FeignException ex) {
        String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
        this.append(errorMsg);
    }

    private void handleGenericException(Exception ex) {
        String errorMsg = ex.getMessage();
        this.append(errorMsg);
    }
}