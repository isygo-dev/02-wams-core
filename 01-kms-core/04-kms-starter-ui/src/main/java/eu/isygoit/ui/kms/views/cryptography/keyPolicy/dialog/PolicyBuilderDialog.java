package eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.KeyPolicy;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PolicyBuilderDialog extends KmsActionDialog {

    private final ObjectMapper objectMapper;
    private final Consumer<KeyPolicy> onSave;
    private final List<KeyPolicy.Statement> statements = new ArrayList<>();
    private final TextField versionField = new TextField(I18n.t("kms.policy.builder.field.version"));
    private final TextField idField = new TextField(I18n.t("kms.policy.builder.field.id"));
    private final RowCardList<KeyPolicy.Statement> statementList = new RowCardList<>();
    private final KeyPolicy policy;

    public PolicyBuilderDialog(ObjectMapper objectMapper, KeyPolicy existingPolicy, Consumer<KeyPolicy> onSave) {
        super(I18n.t("kms.policy.builder.title"), null);
        this.objectMapper = objectMapper;
        this.onSave = onSave;
        this.policy = (existingPolicy != null) ? existingPolicy : createDefaultPolicy();

        setOkButtonText(I18n.t("kms.policy.builder.apply"));
        DialogLayout.size(this, DialogLayout.WIDTH_L);

        buildContent();
    }

    private KeyPolicy createDefaultPolicy() {
        return KeyPolicy.builder()
                .version("2012-10-17")
                .statements(new ArrayList<>())
                .build();
    }

    private void buildContent() {
        VerticalLayout mainLayout = DialogLayout.stack();

        String version = policy.getVersion();
        versionField.setValue(version != null ? version : "2012-10-17");
        versionField.setWidthFull();
        versionField.setHelperText(I18n.t("kms.policy.builder.field.version.helper"));

        String policyId = policy.getId();
        idField.setValue(policyId != null ? policyId : "");
        idField.setWidthFull();
        idField.setHelperText(I18n.t("kms.policy.builder.field.id.helper"));

        FormLayout policyForm = DialogLayout.responsiveForm();
        policyForm.add(versionField, idField);
        mainLayout.add(policyForm);
        VerticalLayout statementsSection = DialogLayout.section(
                I18n.t("kms.policy.builder.statements"), VaadinIcon.LIST);
        mainLayout.add(statementsSection);

        HorizontalLayout toolbar = new HorizontalLayout();
        Button addStatementBtn = new Button(I18n.t("kms.policy.builder.add.statement"), new Icon(VaadinIcon.PLUS));
        addStatementBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        toolbar.add(addStatementBtn);
        statementsSection.add(toolbar);

        statementList.emptyText(I18n.t("kms.policy.builder.empty.error"));
        statementList.cardFactory(this::buildStatementCard);
        statementList.setItems(statements);
        statementsSection.add(statementList);

        if (policy.getStatements() != null) {
            statements.addAll(policy.getStatements());
            refreshStatementList();
        }

        addStatementBtn.addClickListener(e -> editStatement(null, newStatement -> {
            statements.add(newStatement);
            refreshStatementList();
        }));

        add(mainLayout);
    }

    private RowCard buildStatementCard(KeyPolicy.Statement stmt) {
        return RowCard.create()
                .title(stmt.getSid())
                .tag(KmsEnumTag.ofValue(stmt.getEffect(), "kms.enum"))
                .action(VaadinIcon.EDIT, I18n.t("kms.policy.builder.edit.tooltip"),
                        () -> editStatement(stmt, updated -> {
                            int idx = statements.indexOf(stmt);
                            if (idx >= 0) statements.set(idx, updated);
                            refreshStatementList();
                        }))
                .dangerAction(VaadinIcon.TRASH, I18n.t("kms.policy.builder.delete.tooltip"), () -> {
                    statements.remove(stmt);
                    refreshStatementList();
                });
    }

    private void refreshStatementList() {
        statementList.setItems(statements);
    }

    private void editStatement(KeyPolicy.Statement existing, Consumer<KeyPolicy.Statement> onDone) {
        PolicyStatementEditorDialog editor = new PolicyStatementEditorDialog(objectMapper, existing, onDone);
        editor.open();
    }

    @Override
    protected boolean onOk() {
        if (statements.isEmpty()) {
            append(I18n.t("kms.policy.builder.empty.error"));
            return false;
        }

        policy.setVersion(versionField.getValue());
        String idValue = idField.getValue();
        policy.setId(idValue != null && !idValue.isEmpty() ? idValue : null);
        policy.setStatements(statements);

        if (onSave != null) {
            onSave.accept(policy);
        }
        return true;
    }
}