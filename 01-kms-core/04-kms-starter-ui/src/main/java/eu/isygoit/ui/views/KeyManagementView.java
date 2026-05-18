package eu.isygoit.ui.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
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
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos.*;
import eu.isygoit.enums.IEnumKeyOrigin;
import eu.isygoit.enums.IEnumKeySpec;
import eu.isygoit.enums.IEnumKeyUsage;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.MainLayout;
import feign.FeignException;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Route(value = "keys", layout = MainLayout.class)
@PageTitle("Key Management")
@PermitAll
public class KeyManagementView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final Button createButton = new Button("Create key", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>("Status");
    private final ProgressBar loadingBar = new ProgressBar();
    private List<KeyCard> allCards = new ArrayList<>();
    private String currentSearch = "";
    private String currentStatus = "All";
    private List<String> existingAliases = new ArrayList<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService, ObjectMapper objectMapper) {
        this.kmsApiService = kmsApiService;
        this.objectMapper = objectMapper;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-keys-view");

        H2 header = new H2("Key Management");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        cardsContainer.setWidthFull();
        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(true);
        add(cardsContainer);

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        createButton.addClickListener(e -> openCreateKeyDialog());
        refreshButton.addClickListener(e -> {
            loadAliases();
            loadKeys();
        });
        searchField.setPlaceholder("Search by alias or key ID");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
            currentSearch = e.getValue();
            filterCards();
        });

        statusFilter.setItems("All", "Enabled", "Disabled", "PendingDeletion");
        statusFilter.setValue("All");
        statusFilter.addValueChangeListener(e -> {
            currentStatus = e.getValue();
            filterCards();
        });

        loadAliases();
        loadKeys();
    }

    private void loadAliases() {
        try {
            ResponseEntity<ListAliasesResponse> response = kmsApiService.listAliases(100, null);
            ListAliasesResponse aliasesResponse = response.getBody();
            if (aliasesResponse != null && aliasesResponse.getAliases() != null) {
                existingAliases = aliasesResponse.getAliases().stream()
                        .map(ListAliasesResponse.AliasEntry::getAliasName)
                        .collect(Collectors.toList());
            } else {
                existingAliases = new ArrayList<>();
            }
        } catch (Exception e) {
            Notification.show("Failed to load aliases: " + e.getMessage(), 3000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            existingAliases = new ArrayList<>();
        }
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
        searchField.setWidth("280px");
        statusFilter.setWidth("160px");
        statusFilter.setPlaceholder("Filter by status");
        leftGroup.add(searchField, statusFilter);

        HorizontalLayout rightGroup = new HorizontalLayout();
        rightGroup.setSpacing(true);
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh keys");
        rightGroup.add(createButton, refreshButton);

        toolbar.add(leftGroup, rightGroup);
        toolbar.expand(leftGroup);
        return toolbar;
    }

    private void loadKeys() {
        showLoading(true);
        allCards.clear();
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keysResponse = response.getBody();
            if (keysResponse != null && keysResponse.getKeys() != null) {
                for (ListKeysResponse.KeyEntry entry : keysResponse.getKeys()) {
                    try {
                        ResponseEntity<DescribeKeyResponse> descResponse =
                                kmsApiService.describeKey(entry.getKeyId());
                        DescribeKeyResponse describe = descResponse.getBody();
                        if (describe != null && describe.getKeyMetadata() != null) {
                            allCards.add(new KeyCard(entry.getKeyId(), describe.getKeyMetadata()));
                        } else {
                            allCards.add(new KeyCard(entry.getKeyId(), null));
                        }
                    } catch (Exception ex) {
                        allCards.add(new KeyCard(entry.getKeyId(), null));
                    }
                }
            }
            filterCards();
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            showLoading(false);
        }
    }

    private void filterCards() {
        cardsContainer.removeAll();
        List<KeyCard> filtered = allCards.stream()
                .filter(card -> {
                    if (!currentStatus.equals("All")) {
                        String status = card.getStatusText();
                        if (!status.equalsIgnoreCase(currentStatus)) return false;
                    }
                    if (currentSearch != null && !currentSearch.isEmpty()) {
                        String searchLower = currentSearch.toLowerCase();
                        return (card.getAliasOrId().toLowerCase().contains(searchLower) ||
                                card.getKeyId().toLowerCase().contains(searchLower));
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            Div emptyState = new Div();
            emptyState.addClassName(LumoUtility.TextAlignment.CENTER);
            emptyState.addClassName(LumoUtility.Padding.XLARGE);
            Icon emptyIcon = VaadinIcon.KEY.create();
            emptyIcon.setSize("48px");
            emptyIcon.getStyle().set("color", "var(--lumo-secondary-text-color)");
            H4 emptyTitle = new H4("No keys found");
            Paragraph emptyDesc = new Paragraph("Try adjusting your search or filter criteria.");
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

    // =========================================================================
    // Create Key Dialog
    // =========================================================================
    private void openCreateKeyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create new KMS key");
        dialog.setWidth("700px");
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        FormLayout form = new FormLayout();

        // Alias section (unchanged)
        ComboBox<String> aliasCombo = new ComboBox<>("Alias (optional)");
        aliasCombo.setItems(existingAliases);
        aliasCombo.setPlaceholder("Select existing or type new");
        aliasCombo.setAllowCustomValue(true);
        TextField newAliasField = new TextField("New alias name");
        newAliasField.setVisible(false);
        newAliasField.setPlaceholder("alias/my-new-alias");
        aliasCombo.addCustomValueSetListener(e -> {
            newAliasField.setVisible(true);
            newAliasField.setValue(e.getDetail());
        });
        aliasCombo.addValueChangeListener(e -> {
            if (e.getValue() != null && !e.getValue().isEmpty() && !existingAliases.contains(e.getValue())) {
                newAliasField.setVisible(true);
                newAliasField.setValue(e.getValue());
            } else {
                newAliasField.setVisible(false);
                newAliasField.clear();
            }
        });

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setMaxLength(500);

        ComboBox<IEnumKeySpec.Types> keySpecCombo = new ComboBox<>("Key specification");
        keySpecCombo.setItems(IEnumKeySpec.Types.values());
        keySpecCombo.setValue(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
        keySpecCombo.setRequiredIndicatorVisible(true);

        ComboBox<IEnumKeyUsage.Types> keyUsageCombo = new ComboBox<>("Key usage");
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        ComboBox<IEnumKeyOrigin.Types> originCombo = new ComboBox<>("Origin");
        originCombo.setItems(IEnumKeyOrigin.Types.values());
        originCombo.setValue(IEnumKeyOrigin.Types.WAMS_KMS);
        originCombo.setRequiredIndicatorVisible(true);

        // Multi‑region checkbox and conditional fields
        Checkbox multiRegionCheckbox = new Checkbox("Multi-region key");
        TextField primaryRegionField = new TextField("Primary region");
        primaryRegionField.setPlaceholder("e.g., us-east-1");
        primaryRegionField.setValue("us-east-1"); // default, can be changed
        primaryRegionField.setVisible(false);
        TextField replicaRegionsField = new TextField("Replica regions (comma‑separated)");
        replicaRegionsField.setPlaceholder("e.g., eu-west-1,ap-southeast-1");
        replicaRegionsField.setVisible(false);

        multiRegionCheckbox.addValueChangeListener(e -> {
            boolean visible = e.getValue();
            primaryRegionField.setVisible(visible);
            replicaRegionsField.setVisible(visible);
            if (!visible) {
                primaryRegionField.clear();
                replicaRegionsField.clear();
            }
        });

        Checkbox bypassPolicyCheckbox = new Checkbox("Bypass policy lockout safety check");
        TextArea policyField = new TextArea("Policy (JSON)");
        policyField.setPlaceholder("{\n  \"Version\": \"2012-10-17\",\n  \"Statement\": [...]\n}");
        policyField.setWidthFull();
        policyField.setHeight("150px");

        // Tag editor
        VerticalLayout tagsContainer = new VerticalLayout();
        tagsContainer.setSpacing(true);
        tagsContainer.setPadding(false);
        List<HorizontalLayout> tagRows = new ArrayList<>();
        Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
        addTagButton.addClickListener(e -> addTagRow(tagsContainer, tagRows, null, null));
        HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags (random key + value)"), addTagButton);
        tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);
        tagsHeader.setSpacing(true);

        form.add(aliasCombo, newAliasField, descriptionField, keySpecCombo, keyUsageCombo, originCombo,
                multiRegionCheckbox, primaryRegionField, replicaRegionsField,
                bypassPolicyCheckbox, policyField, tagsHeader, tagsContainer);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Footer for error messages
        Span errorSpan = new Span();
        errorSpan.getStyle().set("color", "var(--lumo-error-text-color)");
        errorSpan.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        errorSpan.getStyle().set("margin-right", "auto");
        errorSpan.setVisible(false);

        Button createBtn = new Button("Create", e -> {
            errorSpan.setText("");
            errorSpan.setVisible(false);

            String newAlias = null;
            String existingSelectedAlias = null;
            if (newAliasField.isVisible() && !newAliasField.getValue().isBlank()) {
                newAlias = newAliasField.getValue();
            } else if (aliasCombo.getValue() != null && !aliasCombo.getValue().isBlank()) {
                existingSelectedAlias = aliasCombo.getValue();
            }

            List<CreateKeyRequest.Tag> tags = new ArrayList<>();
            for (HorizontalLayout row : tagRows) {
                TextField keyField = (TextField) row.getComponentAt(0);
                TextField valueField = (TextField) row.getComponentAt(1);
                if (!valueField.getValue().isBlank()) {
                    tags.add(CreateKeyRequest.Tag.builder()
                            .tagKey(keyField.getValue())
                            .tagValue(valueField.getValue())
                            .build());
                }
            }

            // Validate policy JSON
            Map<String, Object> policyMap = null;
            if (!policyField.getValue().isBlank()) {
                try {
                    policyMap = objectMapper.readValue(policyField.getValue(), new TypeReference<>() {});
                } catch (Exception ex) {
                    String errorMsg = "Invalid JSON in policy field: " + ex.getMessage();
                    errorSpan.setText(errorMsg);
                    errorSpan.setVisible(true);
                    Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
            }

            try {
                CreateKeyRequest request = CreateKeyRequest.builder()
                        .keyAlias(StringUtils.hasText(newAlias) ? newAlias : existingSelectedAlias)
                        .description(descriptionField.getValue())
                        .keySpec(keySpecCombo.getValue())
                        .keyUsage(keyUsageCombo.getValue())
                        .origin(originCombo.getValue())
                        .multiRegion(multiRegionCheckbox.getValue())
                        .bypassPolicyLockoutSafetyCheck(bypassPolicyCheckbox.getValue())
                        .policy(policyMap)
                        .tags(tags.isEmpty() ? null : tags)
                        .primaryRegion(primaryRegionField.getValue())
                        .replicaRegions(replicaRegionsField.getValue())
                        .build();

                ResponseEntity<CreateKeyResponse> response = kmsApiService.createKey(request);
                if (!response.getStatusCode().is2xxSuccessful()) {
                    String errorMsg = "Key creation failed: " + (response.getBody() != null ? response.getBody().toString() : "unknown error");
                    errorSpan.setText(errorMsg);
                    errorSpan.setVisible(true);
                    Notification.show(errorMsg, 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                dialog.close();
                Notification.show("Key created successfully", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadAliases();
                loadKeys();

            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show("Creation error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                String errorMsg = ex.getMessage();
                errorSpan.setText(errorMsg);
                errorSpan.setVisible(true);
                Notification.show("Error: " + errorMsg, 5000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setWidthFull();
        footerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        footerLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        footerLayout.add(errorSpan);
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelBtn, createBtn);
        buttonLayout.setSpacing(true);
        footerLayout.add(buttonLayout);

        dialog.getFooter().removeAll();
        dialog.getFooter().add(footerLayout);
        dialog.add(form);
        dialog.open();
    }

    private void addTagRow(VerticalLayout container, List<HorizontalLayout> rows, String existingKey, String existingValue) {
        String randomKey = (existingKey != null) ? existingKey : "tag-" + UUID.randomUUID().toString().substring(0, 8);
        TextField keyField = new TextField();
        keyField.setValue(randomKey);
        keyField.setReadOnly(true);
        keyField.setWidth("150px");
        TextField valueField = new TextField();
        valueField.setValue(existingValue != null ? existingValue : "");
        valueField.setPlaceholder("Tag value");
        valueField.setWidth("250px");
        Button removeBtn = new Button(new Icon(VaadinIcon.TRASH));
        removeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        HorizontalLayout row = new HorizontalLayout(keyField, valueField, removeBtn);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        rows.add(row);
        container.add(row);
        removeBtn.addClickListener(e -> {
            container.remove(row);
            rows.remove(row);
        });
    }

    // =========================================================================
    // Key Card
    // =========================================================================
    private class KeyCard extends VerticalLayout {
        private final String keyId;
        private final CreateKeyResponse.KeyMetadata metadata;
        private final String aliasOrId;
        private final String statusText;

        public KeyCard(String keyId, CreateKeyResponse.KeyMetadata metadata) {
            this.keyId = keyId;
            this.metadata = metadata;
            this.aliasOrId = (metadata != null && metadata.getKeyAlias() != null && !metadata.getKeyAlias().isEmpty())
                    ? metadata.getKeyAlias() : keyId;
            this.statusText = (metadata != null && metadata.getKeyStatus() != null)
                    ? metadata.getKeyStatus().name() : "UNKNOWN";
            buildCard();
        }

        public String getKeyId() { return keyId; }
        public String getAliasOrId() { return aliasOrId; }
        public String getStatusText() { return statusText; }

        private void buildCard() {
            setWidthFull();
            setMargin(false);
            setPadding(true);
            addClassName(LumoUtility.BorderRadius.LARGE);
            addClassName(LumoUtility.Background.BASE);
            addClassName(LumoUtility.BoxShadow.XSMALL);
            getStyle().set("transition", "all 0.2s ease-in-out");
            addClassName("hover:shadow-m");

            HorizontalLayout headerRow = new HorizontalLayout();
            headerRow.setWidthFull();
            headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

            Span titleSpan = new Span(aliasOrId);
            titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
            titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
            titleSpan.addClassName(LumoUtility.TextColor.PRIMARY);

            Span statusChip = new Span(statusText);
            statusChip.addClassName(LumoUtility.FontSize.XSMALL);
            statusChip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
            statusChip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
            statusChip.addClassName(LumoUtility.BorderRadius.LARGE);
            statusChip.getStyle().set("display", "inline-block");
            switch (statusText.toUpperCase()) {
                case "ENABLED":
                    statusChip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
                    break;
                case "DISABLED":
                    statusChip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
                    break;
                case "PENDING_DELETION":
                    statusChip.getStyle().set("background-color", "#FFF4E5").set("color", "#B25600");
                    break;
                default:
                    statusChip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
            }
            HorizontalLayout titleRow = new HorizontalLayout(titleSpan, statusChip);
            titleRow.setAlignItems(FlexComponent.Alignment.CENTER);
            titleRow.setSpacing(true);

            HorizontalLayout buttonBar = new HorizontalLayout();
            buttonBar.setSpacing(true);
            buttonBar.setPadding(false);

            Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit alias & description & tags");
            editBtn.addClickListener(e -> openUpdateDialog());

            Button describeBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View details");
            describeBtn.addClickListener(e -> showKeyDetails());

            Button scheduleDeleteBtn = createIconButton(VaadinIcon.CLOCK, "Schedule deletion");
            scheduleDeleteBtn.addClickListener(e -> openScheduleDeletionDialog());

            Button cancelDeleteBtn = createIconButton(VaadinIcon.REFRESH, "Cancel deletion");
            cancelDeleteBtn.addClickListener(e -> cancelDeletion());
            cancelDeleteBtn.setVisible("PENDING_DELETION".equalsIgnoreCase(statusText));

            Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Permanently delete");
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
            deleteBtn.addClickListener(e -> confirmPermanentDelete());

            buttonBar.add(editBtn, describeBtn, scheduleDeleteBtn, cancelDeleteBtn, deleteBtn);
            headerRow.add(titleRow, buttonBar);
            headerRow.expand(titleRow);
            add(headerRow);

            String descText = (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isEmpty())
                    ? metadata.getDescription() : "No description provided";
            Span descSpan = new Span(descText);
            descSpan.addClassName(LumoUtility.FontSize.SMALL);
            descSpan.addClassName(LumoUtility.TextColor.SECONDARY);
            descSpan.getStyle().set("margin-top", "var(--lumo-space-xs)");
            descSpan.getStyle().set("display", "block");
            add(descSpan);

            HorizontalLayout metaRow = new HorizontalLayout();
            metaRow.setSpacing(true);
            metaRow.addClassName(LumoUtility.FontSize.XSMALL);
            metaRow.addClassName(LumoUtility.TextColor.TERTIARY);
            metaRow.getStyle().set("margin-top", "var(--lumo-space-s)");

            String keySpec = (metadata != null && metadata.getKeySpec() != null) ? metadata.getKeySpec().name() : "N/A";
            String keyUsage = (metadata != null && metadata.getKeyUsage() != null) ? metadata.getKeyUsage().name() : "N/A";
            String created = (metadata != null && metadata.getCreateDate() != null) ?
                    metadata.getCreateDate().toLocalDate().toString() : "Unknown";
            String multiRegion = (metadata != null && metadata.getMultiRegion() != null && metadata.getMultiRegion())
                    ? "🌍 Multi-region" : "📍 Single-region";

            metaRow.add(new Span("Spec: " + keySpec));
            metaRow.add(new Span("•"));
            metaRow.add(new Span("Usage: " + keyUsage));
            metaRow.add(new Span("•"));
            metaRow.add(new Span("Created: " + created));
            metaRow.add(new Span("•"));
            metaRow.add(new Span(multiRegion));
            add(metaRow);
        }

        private Button createIconButton(VaadinIcon icon, String tooltip) {
            Button btn = new Button(new Icon(icon));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            btn.setTooltipText(tooltip);
            return btn;
        }

        // ---------------------------------------------------------------------
        // Edit dialog (alias, description, tags)
        // ---------------------------------------------------------------------
        private void openUpdateDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Edit key alias, description & tags");
            dialog.setWidth("650px");

            FormLayout form = new FormLayout();

            ComboBox<String> aliasCombo = new ComboBox<>("Alias");
            aliasCombo.setItems(existingAliases);
            aliasCombo.setPlaceholder("Select existing alias");
            aliasCombo.setAllowCustomValue(true);
            TextField newAliasField = new TextField("New alias name");
            newAliasField.setVisible(false);
            newAliasField.setPlaceholder("alias/my-new-alias");

            String currentAlias = (metadata != null && metadata.getKeyAlias() != null) ? metadata.getKeyAlias() : "";
            aliasCombo.setValue(currentAlias.isEmpty() ? null : currentAlias);

            aliasCombo.addCustomValueSetListener(e -> {
                newAliasField.setVisible(true);
                newAliasField.setValue(e.getDetail());
            });
            aliasCombo.addValueChangeListener(e -> {
                if (e.getValue() != null && !e.getValue().isEmpty() && !existingAliases.contains(e.getValue())) {
                    newAliasField.setVisible(true);
                    newAliasField.setValue(e.getValue());
                } else {
                    newAliasField.setVisible(false);
                    newAliasField.clear();
                }
            });

            TextArea descriptionField = new TextArea("Description");
            descriptionField.setWidthFull();
            descriptionField.setMaxLength(500);
            String currentDesc = (metadata != null && metadata.getDescription() != null) ? metadata.getDescription() : "";
            descriptionField.setValue(currentDesc);

            VerticalLayout tagsContainer = new VerticalLayout();
            tagsContainer.setSpacing(true);
            tagsContainer.setPadding(false);
            List<HorizontalLayout> tagRows = new ArrayList<>();
            List<ListResourceTagsResponse.Tag> currentTags = fetchKeyTags(keyId);
            for (ListResourceTagsResponse.Tag tag : currentTags) {
                addTagRow(tagsContainer, tagRows, tag.getTagKey(), tag.getTagValue());
            }
            Button addTagButton = new Button("Add tag", new Icon(VaadinIcon.PLUS));
            addTagButton.addClickListener(e -> addTagRow(tagsContainer, tagRows, null, null));
            HorizontalLayout tagsHeader = new HorizontalLayout(new Span("Tags"), addTagButton);
            tagsHeader.setAlignItems(FlexComponent.Alignment.BASELINE);

            form.add(aliasCombo, newAliasField, descriptionField, tagsHeader, tagsContainer);
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

            Button saveBtn = new Button("Save", e -> {
                dialog.close();
                String newAlias = null;
                if (newAliasField.isVisible() && !newAliasField.getValue().isBlank()) {
                    newAlias = newAliasField.getValue();
                } else if (aliasCombo.getValue() != null && !aliasCombo.getValue().isBlank()) {
                    newAlias = aliasCombo.getValue();
                }
                String newDescription = descriptionField.getValue();

                List<CreateKeyRequest.Tag> newTags = new ArrayList<>();
                for (HorizontalLayout row : tagRows) {
                    TextField keyField = (TextField) row.getComponentAt(0);
                    TextField valueField = (TextField) row.getComponentAt(1);
                    if (!valueField.getValue().isBlank()) {
                        newTags.add(CreateKeyRequest.Tag.builder()
                                .tagKey(keyField.getValue())
                                .tagValue(valueField.getValue())
                                .build());
                    }
                }

                try {
                    if (!newDescription.equals(currentDesc)) {
                        UpdateKeyDescriptionRequest descRequest = UpdateKeyDescriptionRequest.builder()
                                .keyId(keyId)
                                .description(newDescription)
                                .build();
                        kmsApiService.updateKeyDescription(keyId, descRequest);
                    }

                    if (newAlias != null && !newAlias.equals(currentAlias)) {
                        if (existingAliases.contains(newAlias)) {
                            UpdateAliasRequest aliasRequest = UpdateAliasRequest.builder()
                                    .aliasName(newAlias)
                                    .targetKeyId(keyId)
                                    .build();
                            kmsApiService.updateAlias(newAlias, aliasRequest);
                        } else {
                            CreateAliasRequest createAliasRequest = CreateAliasRequest.builder()
                                    .aliasName(newAlias)
                                    .targetKeyId(keyId)
                                    .build();
                            kmsApiService.createAlias(createAliasRequest);
                        }
                    }

                    // Remove all existing tags
                    if (!currentTags.isEmpty()) {
                        List<String> keysToRemove = currentTags.stream()
                                .map(ListResourceTagsResponse.Tag::getTagKey)
                                .collect(Collectors.toList());
                        UntagResourceRequest untagRequest = UntagResourceRequest.builder()
                                .keyId(keyId)
                                .tagKeys(keysToRemove)
                                .build();
                        kmsApiService.untagResource(keyId, untagRequest);
                    }

                    // Add new tags
                    if (!newTags.isEmpty()) {
                        List<ListResourceTagsResponse.Tag> tagList = newTags.stream()
                                .map(t -> ListResourceTagsResponse.Tag.builder()
                                        .tagKey(t.getTagKey())
                                        .tagValue(t.getTagValue())
                                        .build())
                                .collect(Collectors.toList());
                        TagResourceRequest tagRequest = TagResourceRequest.builder()
                                .keyId(keyId)
                                .tags(tagList)
                                .build();
                        kmsApiService.tagResource(keyId, tagRequest);
                    }

                    Notification.show("Key updated successfully", 3000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadAliases();
                    loadKeys();
                } catch (Exception ex) {
                    Notification.show("Update failed: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelBtn = new Button("Cancel", e -> dialog.close());
            dialog.getFooter().add(cancelBtn, saveBtn);
            dialog.add(form);
            dialog.open();
        }

        private List<ListResourceTagsResponse.Tag> fetchKeyTags(String keyId) {
            try {
                ResponseEntity<ListResourceTagsResponse> response = kmsApiService.listResourceTags(keyId, 100, null);
                ListResourceTagsResponse tagsResponse = response.getBody();
                if (tagsResponse != null && tagsResponse.getTags() != null) {
                    return tagsResponse.getTags();
                }
            } catch (Exception e) { /* ignore */ }
            return new ArrayList<>();
        }

        // ---------------------------------------------------------------------
        // Key Details Dialog (includes tags and policy)
        // ---------------------------------------------------------------------
        private void showKeyDetails() {
            try {
                ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
                DescribeKeyResponse desc = response.getBody();
                if (desc != null && desc.getKeyMetadata() != null) {
                    CreateKeyResponse.KeyMetadata meta = desc.getKeyMetadata();
                    Dialog detailsDialog = new Dialog();
                    detailsDialog.setHeaderTitle("Key details");
                    detailsDialog.setWidth("700px");

                    VerticalLayout content = new VerticalLayout();
                    content.setSpacing(true);
                    content.add(detailRow("Key ID", meta.getKeyId()),
                            detailRow("WRN", meta.getWrn()),
                            detailRow("Alias", meta.getKeyAlias()),
                            detailRow("Description", meta.getDescription()),
                            detailRow("Status", meta.getKeyStatus() != null ? meta.getKeyStatus().name() : "N/A"),
                            detailRow("Key spec", meta.getKeySpec() != null ? meta.getKeySpec().name() : "N/A"),
                            detailRow("Key usage", meta.getKeyUsage() != null ? meta.getKeyUsage().name() : "N/A"),
                            detailRow("Origin", meta.getOrigin() != null ? meta.getOrigin().name() : "N/A"),
                            detailRow("Creation date", meta.getCreateDate() != null ? meta.getCreateDate().toString() : "N/A"),
                            detailRow("Rotation enabled", meta.getRotationEnabled() != null ? meta.getRotationEnabled().toString() : "N/A"),
                            detailRow("Multi-region", meta.getMultiRegion() != null ? meta.getMultiRegion().toString() : "N/A"));

                    // ========== TAGS (show as chips with key=value) ==========
                    List<ListResourceTagsResponse.Tag> tags = fetchKeyTags(keyId);
                    if (!tags.isEmpty()) {
                        Div tagsContainer = new Div();
                        tagsContainer.getStyle()
                                .set("display", "flex")
                                .set("flex-wrap", "wrap")
                                .set("gap", "var(--lumo-space-xs)")
                                .set("margin-top", "var(--lumo-space-s)");

                        Span label = new Span("Tags: ");
                        label.addClassName(LumoUtility.FontWeight.BOLD);
                        tagsContainer.add(label);

                        for (ListResourceTagsResponse.Tag tag : tags) {
                            Span chip = new Span(tag.getTagKey() + "=" + tag.getTagValue());
                            chip.addClassName(LumoUtility.Padding.Horizontal.SMALL);
                            chip.addClassName(LumoUtility.Padding.Vertical.XSMALL);
                            chip.addClassName(LumoUtility.BorderRadius.LARGE);
                            chip.getStyle()
                                    .set("background-color", "#E9ECEF")
                                    .set("color", "#495057")
                                    .set("white-space", "nowrap");
                            tagsContainer.add(chip);
                        }
                        content.add(tagsContainer);
                    }

                    // ========== POLICY (pretty‑printed JSON) ==========
                    try {
                        ResponseEntity<GetKeyPolicyResponse> policyResponse =
                                kmsApiService.getKeyPolicy(keyId);
                        if (policyResponse.getStatusCode().is2xxSuccessful() && policyResponse.getBody() != null) {
                            Object policyObj = policyResponse.getBody().getPolicy();
                            String prettyPolicy = null;
                            if (policyObj instanceof String) {
                                // Already a JSON string – pretty‑print it
                                Object json = objectMapper.readValue((String) policyObj, Object.class);
                                prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            } else if (policyObj instanceof Map) {
                                // It's a Map (Jackson already parsed it)
                                prettyPolicy = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(policyObj);
                            }
                            if (prettyPolicy != null && !prettyPolicy.isBlank()) {
                                Span label = new Span("Policy: ");
                                label.addClassName(LumoUtility.FontWeight.BOLD);
                                content.add(label);
                                // Use a TextArea to display the formatted JSON
                                TextArea policyArea = new TextArea();
                                policyArea.addClassName(LumoUtility.FontWeight.BOLD);
                                policyArea.setValue(prettyPolicy);
                                policyArea.setWidthFull();
                                policyArea.setHeight("300px");
                                policyArea.setReadOnly(true);
                                policyArea.getStyle().set("font-family", "monospace");
                                content.add(policyArea);
                            }
                        }
                    } catch (Exception e) {
                        // Policy may not exist – ignore
                        log.warn("Could not fetch or format policy for key {}", keyId, e);
                    }

                    detailsDialog.add(content);
                    Button closeBtn = new Button("Close", e -> detailsDialog.close());
                    closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    detailsDialog.getFooter().add(closeBtn);
                    detailsDialog.open();
                } else {
                    Notification.show("No metadata found", 3000, Notification.Position.TOP_END);
                }
            } catch (Exception e) {
                Notification.show("Failed to load details", 3000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }

        private HorizontalLayout detailRow(String label, String value) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            Span labelSpan = new Span(label + ":");
            labelSpan.addClassName(LumoUtility.FontWeight.BOLD);
            labelSpan.setWidth("30%");
            Span valueSpan = new Span(value != null ? value : "-");
            valueSpan.setWidth("70%");
            row.add(labelSpan, valueSpan);
            return row;
        }

        private void openScheduleDeletionDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Schedule key deletion");
            IntegerField daysField = new IntegerField("Pending window (days)");
            daysField.setMin(7);
            daysField.setMax(30);
            daysField.setValue(30);
            daysField.setStepButtonsVisible(true);
            daysField.setWidthFull();

            Button confirmBtn = new Button("Schedule", e -> {
                dialog.close();
                try {
                    ResponseEntity<ScheduleKeyDeletionResponse> response =
                            kmsApiService.scheduleKeyDeletion(keyId, daysField.getValue());
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Deletion scheduled in " + daysField.getValue() + " days", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
                    } else {
                        Notification.show("Failed to schedule deletion", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelBtn = new Button("Cancel", e -> dialog.close());
            dialog.getFooter().add(cancelBtn, confirmBtn);
            dialog.add(daysField);
            dialog.open();
        }

        private void cancelDeletion() {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Cancel deletion");
            confirm.setText("Are you sure you want to cancel the deletion of key " + aliasOrId + "?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Yes, cancel");
            confirm.addConfirmListener(event -> {
                try {
                    ResponseEntity<CancelKeyDeletionResponse> response =
                            kmsApiService.cancelKeyDeletion(keyId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Deletion cancelled", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
                    } else {
                        Notification.show("Failed to cancel deletion", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_END)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirm.open();
        }

        private void confirmPermanentDelete() {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Permanently delete key");
            confirm.setText("This action is irreversible. The key will be permanently removed.");
            confirm.setCancelable(true);
            confirm.setConfirmText("Delete permanently");
            confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            confirm.addConfirmListener(event -> {
                try {
                    ResponseEntity<DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Key permanently deleted", 3000, Notification.Position.TOP_END)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
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
    }
}