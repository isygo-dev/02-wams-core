package eu.isygoit.ui.views.keyTag;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
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
import java.util.stream.Collectors;

@Slf4j
@Route(value = "tags", layout = MainLayout.class)
@PageTitle("Key Tagging")
@PermitAll
public class TagsView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>("KMS Key");
    private final Grid<KmsDtos.ListResourceTagsResponse.Tag> tagsGrid = new Grid<>();
    private final Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
    private final Button addTagButton = new Button("Add Tag", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button removeTagButton = new Button("Remove Selected", new Icon(VaadinIcon.TRASH));
    private final ProgressBar loadingBar = new ProgressBar();

    private List<KeyOption> keyOptions = new ArrayList<>();
    private String selectedKeyId = null;

    @Autowired
    public TagsView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-tags-view");

        H2 header = new H2("Key Tagging");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        add(header);

        // Key selection toolbar
        HorizontalLayout keyLayout = new HorizontalLayout();
        keyLayout.setWidthFull();
        keyLayout.setAlignItems(Alignment.CENTER);
        keyLayout.setSpacing(true);

        keyCombo.setPlaceholder("Select a KMS key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("400px");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                loadTags();
            } else {
                selectedKeyId = null;
                tagsGrid.setItems(new ArrayList<>());
                tagsGrid.setVisible(false);
            }
        });

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.addClickListener(e -> {
            loadKeyOptions();
            if (selectedKeyId != null) loadTags();
        });

        keyLayout.add(keyCombo, refreshButton);
        add(keyLayout);

        // Tags grid – direct child, will expand
        tagsGrid.setWidthFull();
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagKey).setHeader("Tag Key").setSortable(true);
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagValue).setHeader("Tag Value").setSortable(true);
        tagsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        tagsGrid.setVisible(false);
        tagsGrid.setEmptyStateText("No tags found for this key.");
        add(tagsGrid);
        setFlexGrow(1, tagsGrid);

        // Action buttons
        HorizontalLayout actionBar = new HorizontalLayout(addTagButton, removeTagButton);
        actionBar.setSpacing(true);
        addTagButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        removeTagButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        addTagButton.addClickListener(e -> openAddTagDialog());
        removeTagButton.addClickListener(e -> confirmDeleteSelectedTag());
        add(actionBar);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        add(loadingBar);

        // Initial load
        loadKeyOptions();
    }

    // ----- Data loading (unchanged) -----
    private void loadKeyOptions() {
        showLoading(true);
        try {
            ResponseEntity<KmsDtos.ListKeysResponse> response = kmsApiService.listKeys(100, null);
            KmsDtos.ListKeysResponse keys = response.getBody();
            if (keys != null && keys.getKeys() != null) {
                keyOptions = keys.getKeys().stream()
                        .map(entry -> new KeyOption(entry.getKeyId(), fetchAlias(entry.getKeyId())))
                        .collect(Collectors.toList());
                keyCombo.setItems(keyOptions);
            } else {
                keyOptions = new ArrayList<>();
                keyCombo.setItems(keyOptions);
            }
            if (selectedKeyId != null && keyOptions.stream().noneMatch(opt -> opt.getKeyId().equals(selectedKeyId))) {
                selectedKeyId = null;
                keyCombo.clear();
                tagsGrid.setItems(new ArrayList<>());
                tagsGrid.setVisible(false);
            }
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            Notification.show("Failed to load keys: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            Notification.show("Failed to load keys: " + e.getMessage(), 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load keys: {}", e.getMessage());
        } finally {
            showLoading(false);
        }
    }

    private String fetchAlias(String keyId) {
        try {
            ResponseEntity<KmsDtos.DescribeKeyResponse> response = kmsApiService.describeKey(keyId);
            KmsDtos.DescribeKeyResponse desc = response.getBody();
            if (desc != null && desc.getKeyMetadata() != null && StringUtils.hasText(desc.getKeyMetadata().getKeyAlias())) {
                return desc.getKeyMetadata().getKeyAlias();
            }
        } catch (Exception e) {
            // ignore
        }
        return keyId;
    }

    private void loadTags() {
        if (selectedKeyId == null) {
            tagsGrid.setVisible(false);
            return;
        }
        showLoading(true);
        tagsGrid.setVisible(false);
        try {
            ResponseEntity<KmsDtos.ListResourceTagsResponse> response = kmsApiService.listResourceTags(selectedKeyId, 100, null);
            KmsDtos.ListResourceTagsResponse tagsResponse = response.getBody();
            if (tagsResponse != null && tagsResponse.getTags() != null) {
                tagsGrid.setItems(tagsResponse.getTags());
            } else {
                tagsGrid.setItems(new ArrayList<>());
            }
            tagsGrid.setVisible(true);
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            Notification.show("Failed to load tags: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load tags for key {}: {}", selectedKeyId, errorMsg);
            tagsGrid.setItems(new ArrayList<>());
            tagsGrid.setVisible(true);
        } catch (Exception e) {
            Notification.show("Failed to load tags: " + e.getMessage(), 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to load tags for key {}: {}", selectedKeyId, e.getMessage());
            tagsGrid.setItems(new ArrayList<>());
            tagsGrid.setVisible(true);
        } finally {
            showLoading(false);
        }
    }

    private void showLoading(boolean show) {
        loadingBar.setVisible(show);
        addTagButton.setEnabled(!show);
        removeTagButton.setEnabled(!show);
        keyCombo.setEnabled(!show);
        refreshButton.setEnabled(!show);
        tagsGrid.setEnabled(!show);
    }

    // ----- Add tag dialog (unchanged) -----
    private void openAddTagDialog() {
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add tag");
        dialog.setWidth("400px");

        TextField keyField = new TextField("Tag key");
        keyField.setRequired(true);
        keyField.setMaxLength(128);
        keyField.setPlaceholder("e.g., Environment");

        TextField valueField = new TextField("Tag value");
        valueField.setRequired(true);
        valueField.setMaxLength(256);
        valueField.setPlaceholder("e.g., Production");

        Button saveBtn = new Button("Add", e -> {
            String tagKey = keyField.getValue();
            String tagValue = valueField.getValue();
            if (!StringUtils.hasText(tagKey) || !StringUtils.hasText(tagValue)) {
                Notification.show("Both key and value are required", 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            dialog.close();
            try {
                KmsDtos.TagResourceRequest request = KmsDtos.TagResourceRequest.builder()
                        .keyId(selectedKeyId)
                        .tags(List.of(KmsDtos.ListResourceTagsResponse.Tag.builder()
                                .tagKey(tagKey)
                                .tagValue(tagValue)
                                .build()))
                        .build();
                kmsApiService.tagResource(selectedKeyId, request);
                Notification.show("Tag added", 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadTags();
            } catch (FeignException ex) {
                String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
                Notification.show("Failed to add tag: " + errorMsg, 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                log.error("Failed to add tag to key {}: {}", selectedKeyId, errorMsg);
            } catch (Exception ex) {
                Notification.show("Failed to add tag: " + ex.getMessage(), 6000, Notification.Position.TOP_END)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                log.error("Failed to add tag to key {}: {}", selectedKeyId, ex.getMessage());
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancelBtn = new Button("Cancel", e -> dialog.close());

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.add(keyField, valueField);
        dialog.open();
    }

    // ----- Delete selected tag with confirmation -----
    private void confirmDeleteSelectedTag() {
        KmsDtos.ListResourceTagsResponse.Tag selected = tagsGrid.asSingleSelect().getValue();
        if (selected == null) {
            Notification.show("No tag selected", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }
        if (selectedKeyId == null) {
            Notification.show("Please select a key first", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Remove tag");
        confirmDialog.setText("Are you sure you want to remove the tag \"" + selected.getTagKey() + "\" from the key?");
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Remove");
        confirmDialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        confirmDialog.addConfirmListener(event -> deleteSelectedTag(selected.getTagKey()));
        confirmDialog.open();
    }

    private void deleteSelectedTag(String tagKey) {
        try {
            KmsDtos.UntagResourceRequest request = KmsDtos.UntagResourceRequest.builder()
                    .keyId(selectedKeyId)
                    .tagKeys(List.of(tagKey))
                    .build();
            kmsApiService.untagResource(selectedKeyId, request);
            Notification.show("Tag removed", 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            loadTags();
        } catch (FeignException ex) {
            String errorMsg = ex.status() == 500 ? ex.contentUTF8() : ex.getMessage();
            Notification.show("Failed to remove tag: " + errorMsg, 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            Notification.show("Failed to remove tag: " + e.getMessage(), 6000, Notification.Position.TOP_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, e.getMessage());
        }
    }

    // Helper class for key selection (unchanged)
    private static class KeyOption {
        private final String keyId;
        private final String displayName;

        KeyOption(String keyId, String aliasOrId) {
            this.keyId = keyId;
            this.displayName = aliasOrId != null ? aliasOrId + " (" + keyId + ")" : keyId;
        }

        String getKeyId() {
            return keyId;
        }

        String getDisplayName() {
            return displayName;
        }
    }
}