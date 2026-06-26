package eu.isygoit.ui.mms.views.msgtemplate.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.spring.annotation.UIScope;
import eu.isygoit.dto.data.MsgTemplateDto;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.mms.MsgTemplateService;
import feign.FeignException;
import org.springframework.stereotype.Component;

@Component
@UIScope
public class EditTemplateDialog extends Dialog {

    private final MsgTemplateService templateService;
    private final MsgTemplateDto originalTemplate;
    private final Runnable onSuccess;

    private final TextField tenantField = new TextField(I18n.t("template.dialog.tenant"));
    private final ComboBox<String> nameField = new ComboBox<>(I18n.t("template.dialog.name"));
    private final TextField codeField = new TextField(I18n.t("template.dialog.code"));
    private final TextArea descriptionField = new TextArea(I18n.t("template.dialog.description"));
    private final ComboBox<IEnumLanguage.Types> languageField = new ComboBox<>(I18n.t("template.dialog.language"));

    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Upload upload = new Upload(buffer);

    private final Button saveButton = new Button(I18n.t("template.dialog.save"));
    private final Button cancelButton = new Button(I18n.t("template.dialog.cancel"));

    public EditTemplateDialog(MsgTemplateService templateService,
                              MsgTemplateDto template,
                              Runnable onSuccess) {
        this.templateService = templateService;
        this.originalTemplate = template;
        this.onSuccess = onSuccess;

        setHeaderTitle(I18n.t("template.dialog.edit.title", template.getId()));
        setWidth("600px");
        setModal(true);
        setDraggable(true);
        setResizable(true);

        populateFields();

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        layout.add(buildForm());

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttons.setSpacing(true);

        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> close());

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> updateTemplate());

        buttons.add(cancelButton, saveButton);
        layout.add(buttons);

        add(layout);
    }

    private void populateFields() {
        tenantField.setValue(originalTemplate.getTenant() != null ? originalTemplate.getTenant() : "");
        nameField.setValue(originalTemplate.getName());
        codeField.setValue(originalTemplate.getCode() != null ? originalTemplate.getCode() : "");
        descriptionField.setValue(originalTemplate.getDescription() != null ? originalTemplate.getDescription() : "");
        languageField.setValue(originalTemplate.getLanguage() != null ? originalTemplate.getLanguage() : IEnumLanguage.Types.EN);

        tenantField.setReadOnly(true);
        codeField.setReadOnly(true);
    }

    private VerticalLayout buildForm() {
        VerticalLayout form = new VerticalLayout();
        form.setSpacing(true);
        form.setPadding(false);

        tenantField.setWidthFull();
        tenantField.setRequired(true);
        tenantField.setRequiredIndicatorVisible(true);

        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setRequiredIndicatorVisible(true);
        nameField.setItems("WELCOME", "PASSWORD_RESET", "NEWSLETTER", "INVOICE", "NOTIFICATION");

        codeField.setWidthFull();
        codeField.setRequired(true);
        codeField.setRequiredIndicatorVisible(true);

        descriptionField.setWidthFull();
        descriptionField.setHeight("100px");

        languageField.setWidthFull();
        languageField.setItems(IEnumLanguage.Types.values());
        languageField.setRequired(true);
        languageField.setRequiredIndicatorVisible(true);

        upload.setAcceptedFileTypes(".html", ".txt", ".ftl");
        upload.setMaxFileSize(10 * 1024 * 1024);
        upload.setDropAllowed(true);

        form.add(
                tenantField,
                nameField,
                codeField,
                descriptionField,
                languageField,
                upload
        );

        return form;
    }

    private void updateTemplate() {
        if (!validateForm()) {
            return;
        }

        MsgTemplateDto dto = new MsgTemplateDto();
        dto.setId(originalTemplate.getId());
        dto.setTenant(tenantField.getValue());
        dto.setName(nameField.getValue());
        dto.setCode(codeField.getValue());
        dto.setDescription(descriptionField.getValue());
        dto.setLanguage(languageField.getValue());

        // Handle file upload if any
        if (buffer.getFileData() != null) {
            try {
                dto.setFileName(buffer.getFileData().getFileName());
                dto.setOriginalFileName(buffer.getFileData().getFileName());
            } catch (Exception e) {
                // Ignore
            }
        }

        try {
            templateService.update(dto.getId(), dto);
            Notification.show(I18n.t("template.update.success"), 3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            close();
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 400 ? I18n.t("template.update.validation.error") : ex.getMessage();
            Notification.show(I18n.t("template.update.error", errorMsg), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification.show(I18n.t("template.update.error", e.getMessage()), 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (nameField.getValue() == null || nameField.getValue().isEmpty()) {
            errors.append(I18n.t("template.validation.name.required")).append("\n");
        }
        if (languageField.getValue() == null) {
            errors.append(I18n.t("template.validation.language.required")).append("\n");
        }

        if (errors.length() > 0) {
            Notification.show(errors.toString(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        return true;
    }
}