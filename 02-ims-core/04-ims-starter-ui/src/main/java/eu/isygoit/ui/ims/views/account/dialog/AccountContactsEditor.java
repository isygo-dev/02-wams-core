package eu.isygoit.ui.ims.views.account.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.ContactDto;
import eu.isygoit.enums.IEnumContact;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.ims.views.common.ImsEnumTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Editable list of {@link ContactDto} (type + value) used by the account form.
 * Existing contacts keep their identity (id, audit data) when edited.
 */
class AccountContactsEditor extends VerticalLayout {

    private final VerticalLayout rows = new VerticalLayout();
    private final List<Row> items = new ArrayList<>();

    AccountContactsEditor() {
        setPadding(false);
        setSpacing(true);
        setWidthFull();

        rows.setPadding(false);
        rows.setSpacing(true);
        rows.setWidthFull();

        Button addButton = new Button(I18n.t("ims.account.dialog.contact.add"), VaadinIcon.PLUS.create(),
                e -> addRow(null));
        addButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(rows, addButton);
    }

    /** Replaces the edited list with the given contacts. */
    void setContacts(List<ContactDto> contacts) {
        rows.removeAll();
        items.clear();
        if (contacts != null) {
            contacts.forEach(this::addRow);
        }
    }

    /** Current contacts; rows without a value are ignored. */
    List<ContactDto> getContacts() {
        List<ContactDto> result = new ArrayList<>();
        for (Row row : items) {
            String value = row.valueField.getValue() == null ? "" : row.valueField.getValue().trim();
            if (value.isEmpty()) {
                continue;
            }
            ContactDto dto = row.source != null ? row.source : new ContactDto();
            dto.setType(row.typeCombo.getValue());
            dto.setValue(value);
            result.add(dto);
        }
        return result;
    }

    private void addRow(ContactDto source) {
        ComboBox<IEnumContact.Types> typeCombo = new ComboBox<>(I18n.t("ims.account.dialog.contact.type"));
        typeCombo.setItems(IEnumContact.Types.values());
        typeCombo.setItemLabelGenerator(type -> ImsEnumTag.label(type, "ims.enum.contact"));

        TextField valueField = new TextField(I18n.t("ims.account.dialog.contact.value"));
        valueField.setWidthFull();

        if (source != null) {
            typeCombo.setValue(source.getType());
            valueField.setValue(source.getValue() == null ? "" : source.getValue());
        } else {
            typeCombo.setValue(IEnumContact.Types.PHONE);
        }

        Button removeButton = new Button(VaadinIcon.TRASH.create());
        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeButton.setAriaLabel(I18n.t("ims.account.dialog.contact.remove"));
        removeButton.setTooltipText(I18n.t("ims.account.dialog.contact.remove"));

        HorizontalLayout layout = new HorizontalLayout(typeCombo, valueField, removeButton);
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.END);
        layout.setFlexGrow(1, typeCombo);
        layout.setFlexGrow(2, valueField);

        Row row = new Row(source, typeCombo, valueField);
        removeButton.addClickListener(e -> {
            items.remove(row);
            rows.remove(layout);
        });
        items.add(row);
        rows.add(layout);
    }

    private record Row(ContactDto source, ComboBox<IEnumContact.Types> typeCombo, TextField valueField) {
    }
}
