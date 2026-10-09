package eu.isygoit.ui.sms.views.common;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

import java.util.function.Consumer;

/**
 * Single implementation of the "create bucket" form, shared by the two public
 * {@code CreateBucketDialog} classes (bucket and object screens). Only the
 * parent view and the message keys differ between them. The only editable
 * {@link BucketDto} field is {@code name}; region, ARN and creation date are
 * assigned by the storage backend.
 */
public abstract class AbstractCreateBucketDialog extends SmsActionDialog {

    /** Strictest bucket naming rule: 3-63 chars, lowercase letters, digits, dots, hyphens, alphanumeric ends. */
    public static final String BUCKET_NAME_PATTERN = "^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$";
    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 63;

    /** i18n keys used by one concrete dialog; {@code rules} may be {@code null}. */
    public record Messages(String nameLabel,
                           String placeholder,
                           String helper,
                           String rules,
                           String required,
                           String lengthError,
                           String formatError,
                           String success,
                           String failed,
                           String error) {
    }

    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final Consumer<Boolean> loading;
    private final Messages messages;
    private TextField bucketNameField;

    protected AbstractCreateBucketDialog(String title,
                                         String okText,
                                         Runnable onSuccess,
                                         String tenant,
                                         ObjectStorageService objectStorageService,
                                         Consumer<Boolean> loading,
                                         Messages messages) {
        super(title, onSuccess);
        this.tenant = tenant;
        this.objectStorageService = objectStorageService;
        this.loading = loading;
        this.messages = messages;

        setOkButtonText(okText);
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        buildForm();
        setupValidation();
    }

    private void buildForm() {
        bucketNameField = new TextField(I18n.t(messages.nameLabel()));
        bucketNameField.setRequired(true);
        bucketNameField.setRequiredIndicatorVisible(true);
        bucketNameField.setPlaceholder(I18n.t(messages.placeholder()));
        bucketNameField.setHelperText(I18n.t(messages.helper()));
        bucketNameField.setWidthFull();

        FormLayout form = DialogLayout.responsiveForm();
        form.add(bucketNameField);
        form.setColspan(bucketNameField, 2);
        if (messages.rules() != null) {
            Span rules = DialogLayout.help(I18n.t(messages.rules()));
            form.add(rules);
            form.setColspan(rules, 2);
        }
        addContent(form);
    }

    private void setupValidation() {
        Binder<BucketDto> binder = new BeanValidationBinder<>(BucketDto.class);
        binder.forField(bucketNameField)
                .withValidator(new StringLengthValidator(I18n.t(messages.lengthError()),
                        NAME_MIN_LENGTH, NAME_MAX_LENGTH))
                .withValidator(name -> name.matches(BUCKET_NAME_PATTERN), I18n.t(messages.formatError()))
                .bind("name");

        bucketNameField.addValueChangeListener(e -> enableOkButton(isValidName(bucketNameField.getValue())));
        enableOkButton(false);
    }

    private static boolean isValidName(String name) {
        return name != null && !name.isBlank() && name.matches(BUCKET_NAME_PATTERN);
    }

    @Override
    protected boolean onOk() {
        String bucketName = bucketNameField.getValue();
        if (bucketName == null || bucketName.isBlank()) {
            append(I18n.t(messages.required()));
            return false;
        }
        if (!bucketName.matches(BUCKET_NAME_PATTERN)) {
            append(I18n.t(messages.formatError()));
            return false;
        }

        loading.accept(true);
        try {
            ResponseEntity<Object> response = objectStorageService.saveBucket(tenant, bucketName);
            if (!response.getStatusCode().is2xxSuccessful()) {
                append(I18n.t(messages.failed(), response.getStatusCodeValue()));
                return false;
            }
            append(I18n.t(messages.success(), bucketName));
            return true;
        } catch (FeignException ex) {
            append(SmsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t(messages.error(), e.getMessage()));
        } finally {
            loading.accept(false);
        }
        return false;
    }
}
