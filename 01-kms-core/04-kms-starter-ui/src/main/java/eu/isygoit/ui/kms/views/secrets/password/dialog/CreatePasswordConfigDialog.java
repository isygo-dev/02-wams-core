package eu.isygoit.ui.kms.views.secrets.password.dialog;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.PasswordConfigDto;
import eu.isygoit.enums.IEnumAuth;
import eu.isygoit.enums.IEnumCharSet;
import eu.isygoit.remote.kms.PasswordConfigService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import eu.isygoit.ui.common.view.ManagementVerticalView;
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
        addContent(createFormLayout());
    }

    private void buildForm() {
        codeField = new TextField("Code");
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);
        codeField.setPlaceholder("e.g., PWD_PROD");
        codeField.setWidthFull();

        typeCombo = new ComboBox<>("Type");
        typeCombo.setItems(IEnumAuth.Types.values());
        typeCombo.setRequired(true);
        typeCombo.setRequiredIndicatorVisible(true);
        typeCombo.setWidthFull();

        patternField = new TextField("Pattern (regex)");
        patternField.setPlaceholder("e.g., ^[A-Za-z0-9]+$");
        patternField.setWidthFull();

        charSetCombo = new ComboBox<>("Character set");
        charSetCombo.setItems(IEnumCharSet.Types.values());
        charSetCombo.setRequired(true);
        charSetCombo.setWidthFull();

        initialField = new TextField("Initial value");
        initialField.setPlaceholder("Optional starting value");
        initialField.setWidthFull();

        minLengthField = new IntegerField("Min length");
        minLengthField.setValue(8);
        minLengthField.setMin(1);
        minLengthField.setRequired(true);
        minLengthField.setWidthFull();

        maxLengthField = new IntegerField("Max length");
        maxLengthField.setValue(20);
        maxLengthField.setMin(1);
        maxLengthField.setRequired(true);
        maxLengthField.setWidthFull();

        lifeTimeField = new IntegerField("Lifetime (days)");
        lifeTimeField.setValue(90);
        lifeTimeField.setMin(1);
        lifeTimeField.setRequired(true);
        lifeTimeField.setWidthFull();
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
            append("Code is required");
            return false;
        }
        IEnumAuth.Types type = typeCombo.getValue();
        if (type == null) {
            append("Type is required");
            return false;
        }
        IEnumCharSet.Types charSet = charSetCombo.getValue();
        if (charSet == null) {
            append("Character set is required");
            return false;
        }
        Integer minLen = minLengthField.getValue();
        if (minLen == null || minLen < 1) {
            append("Min length must be at least 1");
            return false;
        }
        Integer maxLen = maxLengthField.getValue();
        if (maxLen == null || maxLen < minLen) {
            append("Max length must be >= min length");
            return false;
        }
        Integer lifeTime = lifeTimeField.getValue();
        if (lifeTime == null || lifeTime < 1) {
            append("Lifetime must be at least 1 day");
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
                append("Configuration created successfully");
                return true;
            } else {
                append("Creation failed: " + response.getStatusCode());
                return false;
            }
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
            return false;
        } catch (Exception ex) {
            append("Creation failed: " + ex.getMessage());
            return false;
        }
    }
}