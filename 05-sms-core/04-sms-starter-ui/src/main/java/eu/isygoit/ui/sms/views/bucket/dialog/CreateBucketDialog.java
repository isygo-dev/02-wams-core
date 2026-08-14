package eu.isygoit.ui.sms.views.bucket.dialog;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.sms.views.bucket.BucketManagementView;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateBucketDialog extends BaseActionDialog {

    private final BucketManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final Runnable onSuccess;
    private TextField bucketNameField;
    private Binder<BucketDto> binder;

    public CreateBucketDialog(BucketManagementView parentView, ObjectStorageService objectStorageService,
                              String tenant, Runnable onSuccess) {
        super(I18n.t("sms.buckets.dialog.create.bucket.title"), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("sms.buckets.dialog.create.bucket.button"));
        setWidth("500px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
        setupValidation();
    }

    private void buildForm() {
        bucketNameField = new TextField(I18n.t("sms.buckets.dialog.field.bucket.name"));
        bucketNameField.setRequiredIndicatorVisible(true);
        bucketNameField.setPlaceholder(I18n.t("sms.buckets.dialog.field.bucket.name.placeholder"));
        bucketNameField.setWidthFull();
        bucketNameField.setHelperText(I18n.t("sms.buckets.dialog.field.bucket.name.helper"));
    }

    private void setupValidation() {
        binder = new BeanValidationBinder<>(BucketDto.class);
        binder.forField(bucketNameField)
                .withValidator(new StringLengthValidator(I18n.t("sms.buckets.dialog.field.bucket.name.length.error"), 3, 63))
                .withValidator(name -> name.matches("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$"),
                        I18n.t("sms.buckets.dialog.field.bucket.name.format.error"))
                .bind("name");

        bucketNameField.addValueChangeListener(e -> {
            boolean isValid = bucketNameField.getValue() != null &&
                    !bucketNameField.getValue().isBlank() &&
                    bucketNameField.getValue().length() >= 3 &&
                    bucketNameField.getValue().length() <= 63 &&
                    bucketNameField.getValue().matches("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
            enableOkButton(isValid);
        });
        enableOkButton(false);
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        Span rulesSpan = new Span(I18n.t("sms.buckets.dialog.field.bucket.name.rules"));
        rulesSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        rulesSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        form.add(bucketNameField, rulesSpan);
        return form;
    }

    @Override
    protected boolean onOk() {
        String bucketName = bucketNameField.getValue();
        if (bucketName == null || bucketName.isBlank()) {
            append(I18n.t("sms.buckets.dialog.field.bucket.name.required"));
            return false;
        }
        if (!bucketName.matches("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$")) {
            append(I18n.t("sms.buckets.dialog.field.bucket.name.format.error"));
            return false;
        }

        parentView.showLoading(true);
        try {
            objectStorageService.saveBucket(tenant, bucketName.trim().toLowerCase());
            append(I18n.t("sms.buckets.dialog.create.bucket.success", bucketName));
            if (onSuccess != null) onSuccess.run();
            return true;
        } catch (FeignException ex) {
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.buckets.dialog.create.bucket.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }

    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage() != null ? ex.getMessage() : "Unknown error";
    }
}