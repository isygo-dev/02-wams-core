package eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.isygoit.dto.KmsDtos.KeyPolicy;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PolicyBuilderDialog extends BaseActionDialog {

    private final ObjectMapper objectMapper;
    private final Consumer<KeyPolicy> onSave;
    private final List<KeyPolicy.Statement> statements = new ArrayList<>();
    private final TextField versionField = new TextField(I18n.t("policy.builder.field.version"));
    private final TextField idField = new TextField(I18n.t("policy.builder.field.id"));
    private final Grid<KeyPolicy.Statement> statementGrid = new Grid<>();
    private final KeyPolicy policy;

    public PolicyBuilderDialog(ObjectMapper objectMapper, KeyPolicy existingPolicy, Consumer<KeyPolicy> onSave) {
        super(I18n.t("policy.builder.title"), null);
        this.objectMapper = objectMapper;
        this.onSave = onSave;
        this.policy = (existingPolicy != null) ? existingPolicy : createDefaultPolicy();

        setOkButtonText(I18n.t("policy.builder.apply"));
        setWidth("1000px");
        setMaxWidth("95%");
        setResizable(true);

        buildContent();
    }

    private KeyPolicy createDefaultPolicy() {
        return KeyPolicy.builder()
                .version("2012-10-17")
                .statements(new ArrayList<>())
                .build();
    }

    private void buildContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSpacing(true);
        mainLayout.setPadding(true);
        mainLayout.setWidthFull();

        String version = policy.getVersion();
        versionField.setValue(version != null ? version : "2012-10-17");
        versionField.setWidthFull();
        versionField.setHelperText(I18n.t("policy.builder.field.version.helper"));

        String policyId = policy.getId();
        idField.setValue(policyId != null ? policyId : "");
        idField.setWidthFull();
        idField.setHelperText(I18n.t("policy.builder.field.id.helper"));

        mainLayout.add(versionField, idField);
        mainLayout.add(new H3(I18n.t("policy.builder.statements")));

        HorizontalLayout toolbar = new HorizontalLayout();
        Button addStatementBtn = new Button(I18n.t("policy.builder.add.statement"), new Icon(VaadinIcon.PLUS));
        addStatementBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        toolbar.add(addStatementBtn);
        mainLayout.add(toolbar);

        statementGrid.setItems(statements);
        statementGrid.addColumn(KeyPolicy.Statement::getSid)
                .setHeader(I18n.t("policy.builder.grid.column.sid"))
                .setFlexGrow(1);
        statementGrid.addColumn(KeyPolicy.Statement::getEffect)
                .setHeader(I18n.t("policy.builder.grid.column.effect"))
                .setWidth("100px");
        statementGrid.addComponentColumn(this::createStatementActions)
                .setHeader(I18n.t("policy.builder.grid.column.actions"))
                .setWidth("120px");
        statementGrid.setHeight("350px");
        mainLayout.add(statementGrid);

        if (policy.getStatements() != null) {
            statements.addAll(policy.getStatements());
            refreshStatementGrid();
        }

        addStatementBtn.addClickListener(e -> editStatement(null, newStatement -> {
            statements.add(newStatement);
            refreshStatementGrid();
        }));

        add(mainLayout);
    }

    private Component createStatementActions(KeyPolicy.Statement stmt) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(true);

        Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        editBtn.setTooltipText(I18n.t("policy.builder.edit.tooltip"));
        editBtn.addClickListener(e -> editStatement(stmt, updated -> {
            int idx = statements.indexOf(stmt);
            if (idx >= 0) statements.set(idx, updated);
            refreshStatementGrid();
        }));

        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        deleteBtn.setTooltipText(I18n.t("policy.builder.delete.tooltip"));
        deleteBtn.addClickListener(e -> {
            statements.remove(stmt);
            refreshStatementGrid();
        });

        layout.add(editBtn, deleteBtn);
        return layout;
    }

    private void refreshStatementGrid() {
        statementGrid.getDataProvider().refreshAll();
    }

    private void editStatement(KeyPolicy.Statement existing, Consumer<KeyPolicy.Statement> onDone) {
        PolicyStatementEditorDialog editor = new PolicyStatementEditorDialog(objectMapper, existing, onDone);
        editor.open();
    }

    @Override
    protected boolean onOk() {
        if (statements.isEmpty()) {
            append(I18n.t("policy.builder.empty.error"));
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