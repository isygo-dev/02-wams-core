package eu.isygoit.ui.kms.views.cryptography.keyTag.dialog;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import feign.FeignException;
import org.springframework.util.StringUtils;

import java.util.List;

public class AddTagDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final Runnable onSuccess;

    private TextField keyField;
    private TextField valueField;

    public AddTagDialog(KmsApiService kmsApiService, String keyId, Runnable onSuccess) {
        super(I18n.t("tag.dialog.add.title"), onSuccess);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.onSuccess = onSuccess;

        setOkButtonText(I18n.t("tag.dialog.add.button"));
        setWidth("400px");

        buildForm();
        addContent(keyField, valueField);
    }

    private void buildForm() {
        keyField = new TextField(I18n.t("tag.dialog.field.tag.key"));
        keyField.setRequired(true);
        keyField.setRequiredIndicatorVisible(true);
        keyField.setMaxLength(128);
        keyField.setPlaceholder(I18n.t("tag.dialog.field.tag.key.placeholder"));
        keyField.setHelperText(I18n.t("tag.dialog.field.tag.key.helper"));
        keyField.setWidthFull();

        valueField = new TextField(I18n.t("tag.dialog.field.tag.value"));
        valueField.setRequired(true);
        valueField.setRequiredIndicatorVisible(true);
        valueField.setMaxLength(256);
        valueField.setPlaceholder(I18n.t("tag.dialog.field.tag.value.placeholder"));
        valueField.setHelperText(I18n.t("tag.dialog.field.tag.value.helper"));
        valueField.setWidthFull();
    }

    @Override
    protected boolean onOk() {
        String tagKey = keyField.getValue();
        String tagValue = valueField.getValue();

        if (!StringUtils.hasText(tagKey) || !StringUtils.hasText(tagValue)) {
            append(I18n.t("tag.dialog.both.required"));
            return false;
        }

        try {
            KmsDtos.TagResourceRequest request = KmsDtos.TagResourceRequest.builder()
                    .keyId(keyId)
                    .tags(List.of(KmsDtos.ListResourceTagsResponse.Tag.builder()
                            .tagKey(tagKey)
                            .tagValue(tagValue)
                            .build()))
                    .build();
            kmsApiService.tagResource(keyId, request);
            append(I18n.t("tag.dialog.add.success"));
            return true;
        } catch (FeignException ex) {
            append((ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage());
        } catch (Exception e) {
            append(I18n.t("tag.dialog.add.failed", e.getMessage()));
        }

        return false;
    }
}