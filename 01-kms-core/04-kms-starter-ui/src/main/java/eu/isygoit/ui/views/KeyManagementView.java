package eu.isygoit.ui.views;

import com.vaadin.flow.component.Component;
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
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
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
        refreshButton.addClickListener(e -> loadKeys());
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

        loadKeys();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.getStyle().set("flex-wrap", "wrap"); // fixed: use style instead of setFlexWrap
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
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
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

    private void openCreateKeyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create new KMS key");
        dialog.setWidth("560px");

        FormLayout form = new FormLayout();
        TextField aliasField = new TextField("Alias (optional)");
        aliasField.setPlaceholder("alias/my-key");

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

        form.add(aliasField, descriptionField, keySpecCombo, keyUsageCombo, originCombo);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button createBtn = new Button("Create", e -> {
            dialog.close();
            try {
                CreateKeyRequest request = CreateKeyRequest.builder()
                        .alias(aliasField.getValue())
                        .description(descriptionField.getValue())
                        .keySpec(keySpecCombo.getValue())
                        .keyUsage(keyUsageCombo.getValue())
                        .origin(originCombo.getValue())
                        .build();
                ResponseEntity<CreateKeyResponse> response = kmsApiService.createKey(request);
                if (response.getStatusCode().is2xxSuccessful()) {
                    Notification.show("Key created successfully", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    loadKeys();
                } else {
                    Notification.show("Creation failed", 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.add(form);
        dialog.open();
    }

    // -------------------------------------------------------------------------
    // Professional Key Card (Jira style)
    // -------------------------------------------------------------------------
    private class KeyCard extends VerticalLayout {
        private final String keyId;
        private final CreateKeyResponse.KeyMetadata metadata;
        private final String aliasOrId;
        private final String statusText;

        public KeyCard(String keyId, CreateKeyResponse.KeyMetadata metadata) {
            this.keyId = keyId;
            this.metadata = metadata;
            this.aliasOrId = (metadata != null && metadata.getAlias() != null && !metadata.getAlias().isEmpty())
                    ? metadata.getAlias() : keyId;
            this.statusText = (metadata != null && metadata.getStatus() != null)
                    ? metadata.getStatus().name() : "UNKNOWN";
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

            buttonBar.add(describeBtn, scheduleDeleteBtn, cancelDeleteBtn, deleteBtn);
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
            String created = (metadata != null && metadata.getCreationDate() != null) ?
                    metadata.getCreationDate().toLocalDate().toString() : "Unknown";

            metaRow.add(new Span("Spec: " + keySpec));
            metaRow.add(new Span("•"));
            metaRow.add(new Span("Usage: " + keyUsage));
            metaRow.add(new Span("•"));
            metaRow.add(new Span("Created: " + created));
            add(metaRow);
        }

        private Button createIconButton(VaadinIcon icon, String tooltip) {
            Button btn = new Button(new Icon(icon));
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            btn.setTooltipText(tooltip);
            return btn;
        }

        private void showKeyDetails() {
            try {
                ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
                DescribeKeyResponse desc = response.getBody();
                if (desc != null && desc.getKeyMetadata() != null) {
                    CreateKeyResponse.KeyMetadata meta = desc.getKeyMetadata();
                    Dialog detailsDialog = new Dialog();
                    detailsDialog.setHeaderTitle("Key details");
                    detailsDialog.setWidth("640px");

                    VerticalLayout content = new VerticalLayout();
                    content.setSpacing(true);
                    content.add(detailRow("Key ID", meta.getKeyId()),
                            detailRow("WRN", meta.getWrn()),
                            detailRow("Alias", meta.getAlias()),
                            detailRow("Description", meta.getDescription()),
                            detailRow("Status", meta.getStatus() != null ? meta.getStatus().name() : "N/A"),
                            detailRow("Key spec", meta.getKeySpec() != null ? meta.getKeySpec().name() : "N/A"),
                            detailRow("Key usage", meta.getKeyUsage() != null ? meta.getKeyUsage().name() : "N/A"),
                            detailRow("Origin", meta.getOrigin() != null ? meta.getOrigin().name() : "N/A"),
                            detailRow("Creation date", meta.getCreationDate() != null ? meta.getCreationDate().toString() : "N/A"),
                            detailRow("Rotation enabled", meta.getRotationEnabled() != null ? meta.getRotationEnabled().toString() : "N/A"),
                            detailRow("Multi-region", meta.getMultiRegion() != null ? meta.getMultiRegion().toString() : "N/A"));
                    detailsDialog.add(content);
                    Button closeBtn = new Button("Close", e -> detailsDialog.close());
                    closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    detailsDialog.getFooter().add(closeBtn);
                    detailsDialog.open();
                } else {
                    Notification.show("No metadata found", 3000, Notification.Position.TOP_CENTER);
                }
            } catch (Exception e) {
                Notification.show("Failed to load details", 3000, Notification.Position.TOP_CENTER)
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
                        Notification.show("Deletion scheduled in " + daysField.getValue() + " days", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
                    } else {
                        Notification.show("Failed to schedule deletion", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
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
                        Notification.show("Deletion cancelled", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
                    } else {
                        Notification.show("Failed to cancel deletion", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
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
                        Notification.show("Key permanently deleted", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        loadKeys();
                    } else {
                        Notification.show("Deletion failed", 3000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            confirm.open();
        }
    }
}