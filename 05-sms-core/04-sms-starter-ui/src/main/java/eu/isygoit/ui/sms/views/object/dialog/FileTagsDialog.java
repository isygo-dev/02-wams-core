package eu.isygoit.ui.sms.views.object.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.data.FileTagsDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.sms.ObjectStorageService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import eu.isygoit.ui.sms.views.object.ObjectStorageManagementView;
import feign.FeignException;

import java.util.ArrayList;
import java.util.List;

public class FileTagsDialog extends BaseActionDialog {

    private final ObjectStorageManagementView parentView;
    private final ObjectStorageService objectStorageService;
    private final String tenant;
    private final String bucketName;
    private final ObjectStorageManagementView.FileItem file;

    private VerticalLayout tagsContainer;
    private List<TextField> tagFields = new ArrayList<>();

    public FileTagsDialog(ObjectStorageManagementView parentView, ObjectStorageService objectStorageService,
                          String tenant, String bucketName, ObjectStorageManagementView.FileItem file, Runnable onSuccess) {
        super(I18n.t("sms.objects.dialog.tags.title", file.getFileName()), onSuccess);
        this.parentView = parentView;
        this.objectStorageService = objectStorageService;
        this.tenant = tenant;
        this.bucketName = bucketName;
        this.file = file;

        setOkButtonText(I18n.t("sms.objects.dialog.tags.save"));
        setWidth("500px");
        setMaxWidth("95%");

        buildForm();
        addContent(buildFormLayout());
    }

    private void buildForm() {
        tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);

        if (file.getTags() != null && !file.getTags().isEmpty()) {
            for (String tag : file.getTags()) addTagField(tag);
        } else {
            addTagField("");
        }
    }

    private void addTagField(String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setSpacing(true);
        row.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);

        TextField tagField = new TextField();
        tagField.setValue(value);
        tagField.setPlaceholder(I18n.t("sms.objects.dialog.tags.placeholder"));
        tagField.setWidthFull();
        tagFields.add(tagField);

        Button removeBtn = new Button(I18n.t("sms.objects.dialog.tags.remove"));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        removeBtn.addClickListener(e -> {
            tagsContainer.remove(row);
            tagFields.remove(tagField);
        });

        row.add(tagField, removeBtn);
        tagsContainer.add(row);
    }

    private FormLayout buildFormLayout() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button addTagBtn = new Button(I18n.t("sms.objects.dialog.tags.add"));
        addTagBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addTagBtn.addClickListener(e -> addTagField(""));

        VerticalLayout layout = new VerticalLayout();
        layout.add(tagsContainer, addTagBtn);
        form.add(layout);
        return form;
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
            append(extractErrorMessage(ex));
        } catch (Exception e) {
            append(I18n.t("sms.objects.dialog.tags.error", e.getMessage()));
        } finally {
            parentView.showLoading(false);
        }
        return false;
    }


    private String extractErrorMessage(FeignException ex) {
        try { if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank()) return ex.contentUTF8(); } catch (Exception ignored) {}
        return ex.getMessage();
    }
}