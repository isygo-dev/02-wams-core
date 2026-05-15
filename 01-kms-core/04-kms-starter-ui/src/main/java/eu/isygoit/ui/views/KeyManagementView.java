package eu.isygoit.ui.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
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
    private final Div cardsContainer = new Div();
    private final Button refreshButton = new Button("Refresh");
    private final Button createButton = new Button("Create New Key");

    @Autowired
    public KeyManagementView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header toolbar
        HorizontalLayout toolbar = new HorizontalLayout(createButton, refreshButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        add(toolbar);

        // Cards container – will hold key cards
        cardsContainer.addClassName(LumoUtility.Display.FLEX);
        cardsContainer.addClassName(LumoUtility.FlexWrap.WRAP);
        cardsContainer.addClassName(LumoUtility.Gap.MEDIUM);
        cardsContainer.setWidthFull();
        add(cardsContainer);

        // Action listeners
        createButton.addClickListener(e -> openCreateKeyDialog());
        refreshButton.addClickListener(e -> loadKeys());

        // Initial load
        loadKeys();
    }

    // -------------------------------------------------------------------------
    // Load all keys and display them as cards
    // -------------------------------------------------------------------------
    private void loadKeys() {
        cardsContainer.removeAll();
        try {
            ResponseEntity<ListKeysResponse> response = kmsApiService.listKeys(100, null);
            ListKeysResponse keysResponse = response.getBody();
            if (keysResponse != null && keysResponse.getKeys() != null) {
                List<KeyCard> cards = new ArrayList<>();
                for (ListKeysResponse.KeyEntry entry : keysResponse.getKeys()) {
                    // Fetch full key metadata via describeKey to get alias, description, status, etc.
                    try {
                        ResponseEntity<DescribeKeyResponse> descResponse =
                                kmsApiService.describeKey(entry.getKeyId());
                        DescribeKeyResponse describe = descResponse.getBody();
                        if (describe != null && describe.getKeyMetadata() != null) {
                            cards.add(new KeyCard(entry.getKeyId(), describe.getKeyMetadata()));
                        } else {
                            // Fallback: show minimal info
                            cards.add(new KeyCard(entry.getKeyId(), null));
                        }
                    } catch (Exception ex) {
                        // If describe fails, show a minimal card
                        cards.add(new KeyCard(entry.getKeyId(), null));
                    }
                }
                cards.forEach(cardsContainer::add);
            } else {
                cardsContainer.add(new Div(new com.vaadin.flow.component.html.Span("No keys found.")));
            }
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
        }
    }

    // -------------------------------------------------------------------------
    // Create Key Dialog
    // -------------------------------------------------------------------------
    private void openCreateKeyDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New KMS Key");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        TextField aliasField = new TextField("Alias (optional)");
        aliasField.setPlaceholder("alias/MyKey");

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setMaxLength(500);

        ComboBox<IEnumKeySpec.Types> keySpecCombo = new ComboBox<>("Key Specification");
        keySpecCombo.setItems(IEnumKeySpec.Types.values());
        keySpecCombo.setValue(IEnumKeySpec.Types.SYMMETRIC_DEFAULT);
        keySpecCombo.setRequiredIndicatorVisible(true);

        ComboBox<IEnumKeyUsage.Types> keyUsageCombo = new ComboBox<>("Key Usage");
        keyUsageCombo.setItems(IEnumKeyUsage.Types.values());
        keyUsageCombo.setValue(IEnumKeyUsage.Types.ENCRYPT_DECRYPT);
        keyUsageCombo.setRequiredIndicatorVisible(true);

        ComboBox<IEnumKeyOrigin.Types> originCombo = new ComboBox<>("Origin");
        originCombo.setItems(IEnumKeyOrigin.Types.values());
        originCombo.setValue(IEnumKeyOrigin.Types.WAMS_KMS);
        originCombo.setRequiredIndicatorVisible(true);

        form.add(aliasField, descriptionField, keySpecCombo, keyUsageCombo, originCombo);

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
                    Notification.show("Key created successfully");
                    loadKeys();
                } else {
                    Notification.show("Creation failed: " + response.getStatusCode());
                }
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
            }
        });
        createBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelBtn, createBtn);
        dialog.add(form);
        dialog.open();
    }

    // -------------------------------------------------------------------------
    // Inner class representing a single Key Card
    // -------------------------------------------------------------------------
    private class KeyCard extends VerticalLayout {
        private final String keyId;
        private final CreateKeyResponse.KeyMetadata metadata;

        public KeyCard(String keyId, CreateKeyResponse.KeyMetadata metadata) {
            this.keyId = keyId;
            this.metadata = metadata;
            buildCard();
        }

        private void buildCard() {
            addClassName(LumoUtility.BoxShadow.MEDIUM);
            addClassName(LumoUtility.BorderRadius.LARGE);
            addClassName(LumoUtility.Padding.MEDIUM);
            addClassName(LumoUtility.Background.BASE);
            setWidth("320px");
            setSpacing(false);
            setPadding(true);

            // Title: Alias or KeyId
            String title = (metadata != null && metadata.getAlias() != null && !metadata.getAlias().isEmpty())
                    ? metadata.getAlias() : keyId;
            com.vaadin.flow.component.html.H3 titleLabel = new com.vaadin.flow.component.html.H3(title);
            titleLabel.getStyle().set("margin-top", "0").set("margin-bottom", "0.5rem");

            // Status chip
            String statusText = (metadata != null && metadata.getStatus() != null)
                    ? metadata.getStatus().name() : "UNKNOWN";
            com.vaadin.flow.component.html.Span statusChip = new com.vaadin.flow.component.html.Span(statusText);
            statusChip.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Padding.Horizontal.SMALL,
                    LumoUtility.Padding.Vertical.XSMALL, LumoUtility.BorderRadius.LARGE);
            if ("ENABLED".equalsIgnoreCase(statusText)) {
                statusChip.getStyle().set("background-color", "#e6f4ea").set("color", "#137333");
            } else if ("DISABLED".equalsIgnoreCase(statusText)) {
                statusChip.getStyle().set("background-color", "#fce8e6").set("color", "#c5221f");
            } else {
                statusChip.getStyle().set("background-color", "#f1f3f4").set("color", "#5f6368");
            }

            // Description
            String descText = (metadata != null && metadata.getDescription() != null)
                    ? metadata.getDescription() : "No description";
            com.vaadin.flow.component.html.Span descSpan = new com.vaadin.flow.component.html.Span(descText);
            descSpan.addClassName(LumoUtility.FontSize.SMALL);
            descSpan.addClassName(LumoUtility.TextColor.SECONDARY);

            // Key Spec & Usage
            String keySpec = (metadata != null && metadata.getKeySpec() != null)
                    ? metadata.getKeySpec().name() : "N/A";
            String keyUsage = (metadata != null && metadata.getKeyUsage() != null)
                    ? metadata.getKeyUsage().name() : "N/A";
            com.vaadin.flow.component.html.Span specSpan = new com.vaadin.flow.component.html.Span("Spec: " + keySpec);
            specSpan.addClassName(LumoUtility.FontSize.XSMALL);
            com.vaadin.flow.component.html.Span usageSpan = new com.vaadin.flow.component.html.Span("Usage: " + keyUsage);
            usageSpan.addClassName(LumoUtility.FontSize.XSMALL);

            HorizontalLayout specRow = new HorizontalLayout(specSpan, usageSpan);
            specRow.setSpacing(true);

            // Action buttons
            Button describeBtn = new Button("Describe", e -> showKeyDetails());
            describeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button scheduleDeleteBtn = new Button("Schedule Deletion", e -> openScheduleDeletionDialog());
            scheduleDeleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            Button cancelDeleteBtn = new Button("Cancel Deletion", e -> cancelDeletion());
            cancelDeleteBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
            // Show cancel button only if key is pending deletion
            if (metadata == null || !"PENDING_DELETION".equalsIgnoreCase(statusText)) {
                cancelDeleteBtn.setVisible(false);
            }

            Button deleteBtn = new Button("Delete (Permanent)", e -> confirmPermanentDelete());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
            deleteBtn.getStyle().set("margin-top", "8px");

            HorizontalLayout actionRow = new HorizontalLayout(describeBtn, scheduleDeleteBtn, cancelDeleteBtn);
            actionRow.setSpacing(true);
            actionRow.getStyle().set("flex-wrap", "wrap");

            add(titleLabel, statusChip, descSpan, specRow, actionRow, deleteBtn);
        }

        private void showKeyDetails() {
            try {
                ResponseEntity<DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
                DescribeKeyResponse desc = response.getBody();
                if (desc != null && desc.getKeyMetadata() != null) {
                    CreateKeyResponse.KeyMetadata meta = desc.getKeyMetadata();
                    Dialog detailsDialog = new Dialog();
                    detailsDialog.setHeaderTitle("Key Details: " + keyId);
                    detailsDialog.setWidth("600px");

                    VerticalLayout content = new VerticalLayout();
                    content.add(
                            createDetailRow("Key ID", meta.getKeyId()),
                            createDetailRow("WRN", meta.getWrn()),
                            createDetailRow("Alias", meta.getAlias()),
                            createDetailRow("Description", meta.getDescription()),
                            createDetailRow("Status", meta.getStatus() != null ? meta.getStatus().name() : "N/A"),
                            createDetailRow("Key Spec", meta.getKeySpec() != null ? meta.getKeySpec().name() : "N/A"),
                            createDetailRow("Key Usage", meta.getKeyUsage() != null ? meta.getKeyUsage().name() : "N/A"),
                            createDetailRow("Origin", meta.getOrigin() != null ? meta.getOrigin().name() : "N/A"),
                            createDetailRow("Creation Date", meta.getCreationDate() != null ? meta.getCreationDate().toString() : "N/A"),
                            createDetailRow("Rotation Enabled", meta.getRotationEnabled() != null ? meta.getRotationEnabled().toString() : "N/A"),
                            createDetailRow("Multi-Region", meta.getMultiRegion() != null ? meta.getMultiRegion().toString() : "N/A")
                    );
                    detailsDialog.add(content);
                    Button closeBtn = new Button("Close", e -> detailsDialog.close());
                    detailsDialog.getFooter().add(closeBtn);
                    detailsDialog.open();
                } else {
                    Notification.show("No metadata found for key " + keyId);
                }
            } catch (Exception e) {
                Notification.show("Failed to describe key: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
            }
        }

        private Component createDetailRow(String label, String value) {
            HorizontalLayout row = new HorizontalLayout();
            row.setWidthFull();
            row.setSpacing(true);
            com.vaadin.flow.component.html.Span labelSpan = new com.vaadin.flow.component.html.Span(label + ":");
            labelSpan.addClassName(LumoUtility.FontWeight.BOLD);
            labelSpan.setWidth("30%");
            com.vaadin.flow.component.html.Span valueSpan = new com.vaadin.flow.component.html.Span(value != null ? value : "-");
            valueSpan.setWidth("70%");
            row.add(labelSpan, valueSpan);
            return row;
        }

        private void openScheduleDeletionDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Schedule Key Deletion");
            IntegerField daysField = new IntegerField("Pending window (days)");
            daysField.setMin(7);
            daysField.setMax(30);
            daysField.setValue(30);
            daysField.setStepButtonsVisible(true);

            Button confirmBtn = new Button("Schedule", e -> {
                dialog.close();
                try {
                    ResponseEntity<ScheduleKeyDeletionResponse> response =
                            kmsApiService.scheduleKeyDeletion(keyId, daysField.getValue());
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Key deletion scheduled for " + daysField.getValue() + " days");
                        loadKeys(); // refresh to show updated status
                    } else {
                        Notification.show("Failed to schedule deletion: " + response.getStatusCode());
                    }
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
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
            confirm.setHeader("Cancel deletion?");
            confirm.setText("Are you sure you want to cancel the deletion of key " + keyId + "?");
            confirm.setCancelable(true);
            confirm.setConfirmText("Yes, cancel deletion");
            confirm.addConfirmListener(event -> {
                try {
                    ResponseEntity<CancelKeyDeletionResponse> response =
                            kmsApiService.cancelKeyDeletion(keyId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Deletion cancelled successfully");
                        loadKeys();
                    } else {
                        Notification.show("Failed to cancel deletion: " + response.getStatusCode());
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                }
            });
            confirm.open();
        }

        private void confirmPermanentDelete() {
            ConfirmDialog confirm = new ConfirmDialog();
            confirm.setHeader("Permanently delete key?");
            confirm.setText("This action is irreversible. The key will be permanently removed.");
            confirm.setCancelable(true);
            confirm.setConfirmText("Yes, delete permanently");
            confirm.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
            confirm.addConfirmListener(event -> {
                try {
                    ResponseEntity<DeleteKeyResponse> response = kmsApiService.deleteKey(keyId);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        Notification.show("Key permanently deleted");
                        loadKeys();
                    } else {
                        Notification.show("Deletion failed: " + response.getStatusCode());
                    }
                } catch (Exception e) {
                    Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
                }
            });
            confirm.open();
        }
    }
}