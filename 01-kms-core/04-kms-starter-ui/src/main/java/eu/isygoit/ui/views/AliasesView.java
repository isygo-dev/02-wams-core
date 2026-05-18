package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "aliases", layout = MainLayout.class)
@PageTitle("Key Aliases")
@PermitAll
public class AliasesView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create alias", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ProgressBar loadingBar = new ProgressBar();
    private List<AliasCard> allCards = new ArrayList<>();
    private String currentSearch = "";

    @Autowired
    public AliasesView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-aliases-view");

        // Header
        H2 header = new H2("Key Aliases");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Toolbar
        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        // Cards container
        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        // Loading bar
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateAliasDialog());
        refreshButton.addClickListener(e -> loadAliases());
        searchField.setPlaceholder("Search by alias name or target key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            filterCards();
        });

        loadAliases();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.getStyle().set("flex-wrap", "wrap");
        toolbar.addClassName(LumoUtility.Padding.Bottom.MEDIUM);

        HorizontalLayout leftGroup = new HorizontalLayout();
        leftGroup.setAlignItems(FlexComponent.Alignment.END);
        leftGroup.setSpacing(true);
        searchField.setWidth("300px");
        leftGroup.add(searchField);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh aliases");
        rightGroup.add(createButton, refreshButton);

        toolbar.add(leftGroup, rightGroup);
        toolbar.expand(leftGroup);
        return toolbar;
    }

    private void loadAliases() {
        showLoading(true);
        allCards.clear();
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(100, null);
            ListAliasesResponse aliasesResponse = response.getBody();
            if (aliasesResponse != null && aliasesResponse.getAliases() != null) {
                for (ListAliasesResponse.AliasEntry entry : aliasesResponse.getAliases()) {
                    allCards.add(new AliasCard(entry));
                }
            }
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<AliasCard> filtered = allCards.stream()
                .filter(card -> {
                    if (currentSearch == null || currentSearch.isEmpty()) return true;
                    String searchLower = currentSearch.toLowerCase();
                    return card.getAliasName().toLowerCase().contains(searchLower) ||
                            card.getTargetKeyId().toLowerCase().contains(searchLower);
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.TAG.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No aliases found");
            Paragraph emptyDesc = new Paragraph("Create an alias to give your KMS keys friendly names.");
            emptyDesc.addClassName(LumoUtility.TextColor.SECONDARY);
            emptyState.add(emptyIcon, emptyTitle, emptyDesc);
            cardsContainer.add(emptyState);
        } else {
            filtered.forEach(cardsContainer::add);
        }
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        cardsContainer.setVisible(!show);
        refreshButton.setEnabled(!show);
        createButton.setEnabled(!show);
    }

    private void openCreateAliasDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create alias");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();
        TextField aliasNameField = new TextField("Alias name");
        aliasNameField.setPlaceholder("alias/my-key-alias");
        aliasNameField.setRequiredIndicatorVisible(true);

        // Load KMS keys for selection
        ComboBox<String> targetKeyCombo = new ComboBox<>("Target KMS key");
        targetKeyCombo.setRequiredIndicatorVisible(true);
        targetKeyCombo.setPlaceholder("Select a key...");
        targetKeyCombo.setItems(fetchKeyIds());
        targetKeyCombo.setItemLabelGenerator(keyId -> {
            try {
                ResponseEntity<DescribeKeyResponse> desc = kmsApiService.describeKey(keyId);
                DescribeKeyResponse descBody = desc.getBody();
                if (descBody != null && descBody.getKeyMetadata() != null) {
                    String alias = descBody.getKeyMetadata().getKeyAlias();
                    if (alias != null && !alias.isEmpty()) return alias + " (" + keyId + ")";
                }
            } catch (Exception ignored) {}
            return keyId;
        });

        form.add(aliasNameField, targetKeyCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button createBtn = new Button("Create", e -> {
            String aliasName = aliasNameField.getValue();
            String targetKeyId = targetKeyCombo.getValue();
            if (aliasName == null || aliasName.isBlank()) {
                Notification.show("Alias name is required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (targetKeyId == null || targetKeyId.isBlank()) {
                Notification.show("Target key is required", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            dialog.close();
            try {
                CreateAliasRequest request = CreateAliasRequest.builder()
                        .aliasName(aliasName)
                        .targetKeyId(targetKeyId)
                        .build();
                ResponseEntity<CreateAliasResponse> response = kmsApiService.createAlias(request);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Alias created successfully", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadAliases();
                } else {
                    Notification.show("Creation failed: " + response.getStatusCode(), 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.add(form);
        dialog.open();
    }

    private List<String> fetchKeyIds() {
        List<String> keyIds = new ArrayList<>();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyIds = keys.getKeys().stream()
                        .map(ListKeysResponse.KeyEntry::getKeyId)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            Notification.show("Could not load keys: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        return keyIds;
    }

    // -------------------------------------------------------------------------
    // Alias Card (Jira style)
    // -------------------------------------------------------------------------
    private class AliasCard extends VerticalLayout {
        private final String aliasName;
        private final String targetKeyId;
        private final String aliasWrn;
        private final String createDate;

        public AliasCard(ListAliasesResponse.AliasEntry entry) {
            this.aliasName = entry.getAliasName();
            this.targetKeyId = entry.getTargetKeyId();
            this.aliasWrn = entry.getAliasWrn();
            this.createDate = entry.getCreateDate();
            buildCard();
        }

        public String getAliasName() { return aliasName; }
        public String getTargetKeyId() { return targetKeyId; }

        private void buildCard() {
            setWidthFull();
            setMargin(false);
            setPadding(true);
            addClassName(LumoUtility.BorderRadius.LARGE);
            addClassName(LumoUtility.Background.BASE);
            addClassName(LumoUtility.BoxShadow.XSMALL);
            getStyle().set("transition", "all 0.2s ease-in-out");
            addClassName("hover:shadow-m");

            // Header row: alias name + icon buttons
            HorizontalLayout headerRow = new HorizontalLayout();
            headerRow.setWidthFull();
            headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

            Span aliasSpan = new Span(aliasName);
            aliasSpan.addClassName(LumoUtility.FontWeight.BOLD);
            aliasSpan.addClassName(LumoUtility.FontSize.MEDIUM);
            aliasSpan.addClassName(LumoUtility.TextColor.PRIMARY);

            HorizontalLayout buttonBar = new HorizontalLayout();
            buttonBar.setSpacing(true);

            // Delete alias button
            Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete alias");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmDeleteAlias());

            // Optional: Update alias (reassign to another key) – extra feature
            Button updateBtn = createIconButton(VaadinIcon.EDIT, "Reassign alias");
            updateBtn.addClickListener(e -> openUpdateAliasDialog());

            buttonBar.add(updateBtn, deleteBtn);
            headerRow.add(aliasSpan, buttonBar);
            headerRow.expand(aliasSpan);
            add(headerRow);

            // Details: target key ID
            Span targetSpan = new Span("Target key: " + targetKeyId);
            targetSpan.addClassName(LumoUtility.FontSize.SMALL);
            targetSpan.addClassName(LumoUtility.TextColor.SECONDARY);
            add(targetSpan);

            // Optional: creation date
            if (createDate != null && !createDate.isEmpty()) {
                Span dateSpan = new Span("Created: " + createDate);
                dateSpan.addClassName(LumoUtility.FontSize.XSMALL);
                dateSpan.addClassName(LumoUtility.TextColor.TERTIARY);
                add(dateSpan);
            }
        }

        private Button createIconButton(VaadinIcon icon, String tooltip) {
            Button btn = new Button(new Icon(icon));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            btn.setTooltipText(tooltip);
            return btn;
        }

        private void confirmDeleteAlias() {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Delete alias");
            confirm.setText("Are you sure you want to delete alias '" + aliasName + "'?\nThe underlying KMS key will not be affected.");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete");
            confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            confirm.addConfirmListener(event -> {
                try {
                    DeleteAliasRequest request = DeleteAliasRequest.builder()
                            .aliasName(aliasName)
                            .build();
                    ResponseEntity<DeleteAliasResponse> response = kmsApiService.deleteAlias(aliasName);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Alias deleted", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadAliases();
                    } else {
                        Notification.show("Deletion failed", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirm.open();
        }

        private void openUpdateAliasDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Reassign alias");
            dialog.setWidth("500px");

            ComboBox<String> targetKeyCombo = new ComboBox<>("New target KMS key");
            targetKeyCombo.setRequiredIndicatorVisible(true);
            targetKeyCombo.setPlaceholder("Select a key...");
            targetKeyCombo.setItems(fetchKeyIds());
            targetKeyCombo.setItemLabelGenerator(keyId -> {
                try {
                    ResponseEntity<DescribeKeyResponse> desc = kmsApiService.describeKey(keyId);
                    DescribeKeyResponse descBody = desc.getBody();
                    if (descBody != null && descBody.getKeyMetadata() != null) {
                        String alias = descBody.getKeyMetadata().getKeyAlias();
                        if (alias != null && !alias.isEmpty()) return alias + " (" + keyId + ")";
                    }
                } catch (Exception ignored) {}
                return keyId;
            });
            targetKeyCombo.setValue(targetKeyId); // pre-select current

            Button updateBtn = new Button("Update", e -> {
                String newTargetId = targetKeyCombo.getValue();
                if (newTargetId == null || newTargetId.isBlank()) {
                    Notification.show("Please select a target key", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                dialog.close();
                try {
                    UpdateAliasRequest request = UpdateAliasRequest.builder()
                            .aliasName(aliasName)
                            .targetKeyId(newTargetId)
                            .build();
                    ResponseEntity<UpdateAliasResponse> response = kmsApiService.updateAlias(aliasName, request);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Alias reassigned", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadAliases();
                    } else {
                        Notification.show("Update failed", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            updateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelBtn = new Button("Cancel", e -> dialog.close());
            dialog.getFooter().add(cancelBtn, updateBtn);
            dialog.add(targetKeyCombo);
            dialog.open();
        }
    }
}