package eu.isygoit.ui.kms.views.cryptography.keyTag;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.kms.layout.KmsMainLayout;
import eu.isygoit.ui.kms.views.cryptography.keyTag.dialog.AddTagDialog;
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
@Route(value = "kms/tags", layout = KmsMainLayout.class)
@PageTitle("Key Tagging")
@PermitAll
public class TagsView extends VerticalLayout {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>();
    private final Grid<KmsDtos.ListResourceTagsResponse.Tag> tagsGrid = new Grid<>();
    private final Button addTagButton = new Button("Add Tag", new Icon(VaadinIcon.PLUS_CIRCLE));
    private final Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH));
    private final TextField searchField = new TextField();
    private final ProgressBar loadingBar = new ProgressBar();

    private List<KeyOption> keyOptions = new ArrayList<>();
    private String selectedKeyId = null;
    private ListDataProvider<KmsDtos.ListResourceTagsResponse.Tag> tagsDataProvider;

    @Autowired
    public TagsView(KmsApiService kmsApiService) {
        this.kmsApiService = kmsApiService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("kms-tags-view");

        // Header
        H2 header = new H2("Key Tagging");
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        header.addClassName(LumoUtility.Margin.Top.NONE);
        add(header);

        // --- Toolbar (aligned with KeyManagementView) ---
        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        // Tags grid with per-row delete button
        tagsGrid.setWidthFull();
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagKey)
                .setHeader("Tag Key")
                .setSortable(true)
                .setResizable(true);
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagValue)
                .setHeader("Tag Value")
                .setSortable(true)
                .setResizable(true);
        tagsGrid.addComponentColumn(tag -> {
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteBtn.setTooltipText("Remove this tag");
            deleteBtn.addClickListener(e -> confirmDeleteTag(tag));
            return deleteBtn;
        }).setHeader("Actions").setWidth("80px").setFlexGrow(0);
        tagsGrid.setVisible(false);
        tagsGrid.setEmptyStateText("No tags found for this key.");
        tagsGrid.addClassName("tags-grid");
        add(tagsGrid);
        setFlexGrow(1, tagsGrid);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        loadingBar.addClassName(LumoUtility.Margin.Top.MEDIUM);
        add(loadingBar);

        injectResponsiveStyles();
        loadKeyOptions();
    }

    private HorizontalLayout buildToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(true);
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.addClassName("tags-toolbar");

        // Left group: key selector + tag search
        Span keyLabel = new Span("KMS Key");
        keyLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        keyLabel.addClassName(LumoUtility.TextColor.PRIMARY);

        keyCombo.setPlaceholder("Select a KMS key...");
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("250px");
        keyCombo.setTooltipText("Choose a KMS key to view or manage its tags");
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                loadTags();
            } else {
                selectedKeyId = null;
                clearTagsGrid();
            }
        });

        searchField.setPlaceholder("Filter tags by key or value...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setTooltipText("Type to filter tags by key or value");
        searchField.addValueChangeListener(e -> filterTags());

        HorizontalLayout leftGroup = new HorizontalLayout(keyLabel, keyCombo, searchField);
        leftGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        leftGroup.setSpacing(true);
        leftGroup.setFlexGrow(1, keyCombo); // give combo more space

        // Center group: empty (to match three-group layout of KeyManagementView)
        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setWidthFull();
        centerGroup.setVisible(false); // invisible but maintains structure

        // Right group: refresh + add tag
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText("Refresh keys and tags");
        refreshButton.addClickListener(e -> {
            loadKeyOptions();
            if (selectedKeyId != null) loadTags();
        });

        addTagButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTagButton.setTooltipText("Add a new tag to the selected key");
        addTagButton.addClickListener(e -> openAddTagDialog());

        HorizontalLayout rightGroup = new HorizontalLayout(refreshButton, addTagButton);
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.setSpacing(true);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        toolbar.setFlexGrow(1, centerGroup); // allow center to absorb space
        return toolbar;
    }

    private void injectResponsiveStyles() {
        String css = """
                .tags-toolbar {
                    display: flex;
                    flex-wrap: wrap;
                    gap: var(--lumo-space-s);
                    width: 100%;
                }
                @media (max-width: 768px) {
                    .tags-toolbar {
                        flex-direction: column;
                        align-items: stretch;
                    }
                    .tags-toolbar > * {
                        width: 100% !important;
                        justify-content: center;
                    }
                }
                """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    private void clearTagsGrid() {
        tagsDataProvider = null;
        tagsGrid.setItems(new ArrayList<>());
        tagsGrid.setVisible(false);
        searchField.clear();
        searchField.setEnabled(false);
    }

    private void filterTags() {
        if (tagsDataProvider == null) return;
        String filterText = searchField.getValue().trim().toLowerCase();
        if (filterText.isEmpty()) {
            tagsDataProvider.clearFilters();
        } else {
            tagsDataProvider.setFilter(tag ->
                    tag.getTagKey().toLowerCase().contains(filterText) ||
                            tag.getTagValue().toLowerCase().contains(filterText)
            );
        }
    }

    // ----- Data loading -----
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
                clearTagsGrid();
            }
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Failed to load keys: " + errorMsg);
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showError("Failed to load keys: " + e.getMessage());
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
            clearTagsGrid();
            return;
        }
        showLoading(true);
        tagsGrid.setVisible(false);
        searchField.setEnabled(false);
        try {
            ResponseEntity<KmsDtos.ListResourceTagsResponse> response = kmsApiService.listResourceTags(selectedKeyId, 100, null);
            KmsDtos.ListResourceTagsResponse tagsResponse = response.getBody();
            List<KmsDtos.ListResourceTagsResponse.Tag> tags = (tagsResponse != null && tagsResponse.getTags() != null)
                    ? tagsResponse.getTags()
                    : new ArrayList<>();
            tagsDataProvider = DataProvider.ofCollection(tags);
            tagsGrid.setDataProvider(tagsDataProvider);
            tagsGrid.setVisible(true);
            searchField.setEnabled(true);
            filterTags(); // apply any existing filter
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Failed to load tags: " + errorMsg);
            log.error("Failed to load tags for key {}: {}", selectedKeyId, errorMsg);
            tagsGrid.setItems(new ArrayList<>());
            tagsGrid.setVisible(true);
        } catch (Exception e) {
            showError("Failed to load tags: " + e.getMessage());
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
        keyCombo.setEnabled(!show);
        refreshButton.setEnabled(!show);
        if (!show && selectedKeyId == null) {
            searchField.setEnabled(false);
        } else if (!show && selectedKeyId != null) {
            searchField.setEnabled(true);
        }
        tagsGrid.setEnabled(!show);
    }

    private void showError(String message) {
        Notification.show(message, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification.show(message, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showWarning(String message) {
        Notification.show(message, 6000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    // ----- Add tag dialog -----
    private void openAddTagDialog() {
        if (selectedKeyId == null) {
            showWarning("Please select a key first");
            return;
        }
        new AddTagDialog(kmsApiService, selectedKeyId, this::loadTags).open();
    }

    // ----- Delete tag (per row) -----
    private void confirmDeleteTag(KmsDtos.ListResourceTagsResponse.Tag tag) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Remove tag");
        confirmDialog.setText("Are you sure you want to remove the tag \"" + tag.getTagKey() + "\" from the key?");
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText("Remove");
        confirmDialog.setConfirmButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName());
        confirmDialog.addConfirmListener(event -> deleteTag(tag.getTagKey()));
        confirmDialog.open();
    }

    private void deleteTag(String tagKey) {
        try {
            KmsDtos.UntagResourceRequest request = KmsDtos.UntagResourceRequest.builder()
                    .keyId(selectedKeyId)
                    .tagKeys(List.of(tagKey))
                    .build();
            kmsApiService.untagResource(selectedKeyId, request);
            showSuccess("Tag removed");
            loadTags();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError("Failed to remove tag: " + errorMsg);
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showError("Failed to remove tag: " + e.getMessage());
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, e.getMessage());
        }
    }

    // Helper class
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