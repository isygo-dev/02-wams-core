package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.FileTagsDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.sms.views.common.SmsActionDialog;
import eu.isygoit.ui.sms.views.common.SmsDialogSupport;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits the tags of one stored object. Only {@link FileTagsDto#getTags()} is
 * editable; tenant, bucket, path and file name come from the calling context.
 */
public class FileTagsDialog extends SmsActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final ObjectStorageManagementView.FileItem file;

    private VerticalLayout tagsContainer;
    private final List<TextField> tagFields = new ArrayList<>();

    public FileTagsDialog(ObjectStorageManagementView parentView, ObjectStorageService objectStorageService,
                          String tenant, String bucketName, ObjectStorageManagementView.FileItem file, Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.tags.title", file.getFileName()), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.file = file;

        setOkButtonText(I18n.t("sms.objects.dialog.tags.save"));
        DialogLayout.size(this, DialogLayout.WIDTH_S);

        buildForm();
    }

    private void buildForm() {
        tagsContainer = DialogLayout.stack();

        if (file.getTags() != null && !file.getTags().isEmpty()) {
            for (String tag : file.getTags()) addTagField(tag);
        } else {
            addTagField("");
        }

        Button addTagBtn = new Button(I18n.t("sms.objects.dialog.tags.add"));
        addTagBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addTagBtn.addClickListener(e -> addTagField(""));

        VerticalLayout stack = DialogLayout.stack();
        stack.add(tagsContainer, addTagBtn);
        addContent(stack);
    }

    private void addTagField(String value) {
        TextField tagField = new TextField();
        tagField.setValue(value);
        tagField.setPlaceholder(I18n.t("sms.objects.dialog.tags.placeholder"));
        tagField.getElement().setAttribute("aria-label", I18n.t("sms.objects.dialog.tags.placeholder"));
        tagField.setWidthFull();
        tagFields.add(tagField);

        Button removeBtn = new Button(I18n.t("sms.objects.dialog.tags.remove"));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

        HorizontalLayout row = new HorizontalLayout(tagField, removeBtn);
        row.setWidthFull();
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, tagField);

        removeBtn.addClickListener(e -> {
            tagsContainer.remove(row);
            tagFields.remove(tagField);
        });
        tagsContainer.add(row);
    }

    @Override
    protected boolean onOk() {
        List<String> tags = new ArrayList<>();
        for (TextField field : tagFields) {
            String value = field.getValue();
            if (value != null && !value.isBlank()) tags.add(value.trim());
        }

        parentView.showLoading(true);
        try {
            FileTagsDto dto = new FileTagsDto();
            dto.setTenant(tenant);
            dto.setBucketName(bucketName);
            dto.setPath(file.getPath());
            dto.setFiletName(file.getFileName());
            dto.setTags(tags);
            objectStorageService.updateTags(dto);
            append(I18n.t("sms.objects.dialog.tags.success"));
            return true;
        } catch (FeignException ex) {
            append(SmsDialogSupport.extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.tags.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }
}
