package eu.isygoit.ui.kms.views.cryptography.keyTag;

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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.view.ManagementVerticalView;
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
@VaadinSessionScope
@Route(value = "kms/tags", layout = KmsMainLayout.class)
@PageTitle("Key Tagging")
@PermitAll
public class TagsView extends ManagementVerticalView {

    private final KmsApiService kmsApiService;
    private final ComboBox<KeyOption> keyCombo = new ComboBox<>();
    private final Grid<KmsDtos.ListResourceTagsResponse.Tag> tagsGrid = new Grid<>();
    private final Button addTagButton = new Button(I18n.t("kms.tags.view.add.tag.button"), new Icon(VaadinIcon.PLUS_CIRCLE));
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
        H2 header = new H2(I18n.t("kms.tags.view.title"));
        header.addClassName(LumoUtility.FontSize.XXLARGE);
        header.addClassName(LumoUtility.Margin.Bottom.NONE);
        header.addClassName(LumoUtility.Margin.Top.NONE);
        add(header);

        // --- Toolbar ---
        HorizontalLayout toolbar = buildToolbar();
        add(toolbar);

        // Tags grid
        tagsGrid.setWidthFull();
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagKey)
                .setHeader(I18n.t("kms.tags.view.grid.column.key"))
                .setSortable(true)
                .setResizable(true);
        tagsGrid.addColumn(KmsDtos.ListResourceTagsResponse.Tag::getTagValue)
                .setHeader(I18n.t("kms.tags.view.grid.column.value"))
                .setSortable(true)
                .setResizable(true);
        tagsGrid.addComponentColumn(tag -> {
            Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            deleteBtn.setTooltipText(I18n.t("kms.tags.view.remove.tag"));
            deleteBtn.addClickListener(e -> confirmDeleteTag(tag));
            return deleteBtn;
        }).setHeader(I18n.t("kms.tags.view.grid.column.actions")).setWidth("80px").setFlexGrow(0);
        tagsGrid.setVisible(false);
        tagsGrid.setEmptyStateText(I18n.t("kms.tags.view.grid.empty"));
        tagsGrid.addClassName("tags-grid");
        add(tagsGrid);
        setFlexGrow(1, tagsGrid);

        // Loading indicator
        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidth("200px");
        loadingBar.addClassName(LumoUtility.Margin.Top.MEDIUM);
        add(loadingBar);

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

        // Left group
        Span keyLabel = new Span(I18n.t("kms.tags.view.key.label"));
        keyLabel.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        keyLabel.addClassName(LumoUtility.TextColor.PRIMARY);

        keyCombo.setPlaceholder(I18n.t("kms.tags.view.select.key"));
        keyCombo.setItemLabelGenerator(KeyOption::getDisplayName);
        keyCombo.setWidth("250px");
        keyCombo.setTooltipText(I18n.t("kms.tags.view.select.key"));
        keyCombo.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                selectedKeyId = e.getValue().getKeyId();
                loadTags();
            } else {
                selectedKeyId = null;
                clearTagsGrid();
            }
        });

        searchField.setPlaceholder(I18n.t("kms.tags.view.filter.placeholder"));
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setTooltipText(I18n.t("kms.tags.view.filter.placeholder"));
        searchField.addValueChangeListener(e -> filterTags());

        HorizontalLayout leftGroup = new HorizontalLayout(keyLabel, keyCombo, searchField);
        leftGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        leftGroup.setSpacing(true);
        leftGroup.setFlexGrow(1, keyCombo);

        // Center group: empty
        HorizontalLayout centerGroup = new HorizontalLayout();
        centerGroup.setWidthFull();
        centerGroup.setVisible(false);

        // Right group
        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshButton.setTooltipText(I18n.t("kms.tags.view.refresh.tooltip"));
        refreshButton.addClickListener(e -> {
            loadKeyOptions();
            if (selectedKeyId != null) loadTags();
        });

        addTagButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTagButton.setTooltipText(I18n.t("kms.tags.view.add.tag.tooltip"));
        addTagButton.addClickListener(e -> openAddTagDialog());

        HorizontalLayout rightGroup = new HorizontalLayout(refreshButton, addTagButton);
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.setSpacing(true);

        toolbar.add(leftGroup, centerGroup, rightGroup);
        toolbar.setFlexGrow(1, centerGroup);
        return toolbar;
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
            showError(I18n.t("kms.tags.view.load.keys.error", errorMsg));
            log.error("Failed to load keys: {}", errorMsg);
        } catch (Exception e) {
            showError(I18n.t("kms.tags.view.load.keys.error", e.getMessage()));
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
            filterTags();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.tags.view.load.tags.error", errorMsg));
            log.error("Failed to load tags for key {}: {}", selectedKeyId, errorMsg);
            tagsGrid.setItems(new ArrayList<>());
            tagsGrid.setVisible(true);
        } catch (Exception e) {
            showError(I18n.t("kms.tags.view.load.tags.error", e.getMessage()));
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

    private void openAddTagDialog() {
        if (selectedKeyId == null) {
            showWarning(I18n.t("kms.tags.view.select.key.warning"));
            return;
        }
        new AddTagDialog(kmsApiService, selectedKeyId, this::loadTags).open();
    }

    private void confirmDeleteTag(KmsDtos.ListResourceTagsResponse.Tag tag) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(I18n.t("kms.tag.delete.confirm.title"));
        confirmDialog.setText(I18n.t("kms.tag.delete.confirm.message", tag.getTagKey()));
        confirmDialog.setCancelable(true);
        confirmDialog.setConfirmText(I18n.t("kms.tag.delete.confirm.button"));
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
            showSuccess(I18n.t("kms.tag.delete.success"));
            loadTags();
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            showError(I18n.t("kms.tag.delete.failed", errorMsg));
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, errorMsg);
        } catch (Exception e) {
            showError(I18n.t("kms.tag.delete.failed", e.getMessage()));
            log.error("Failed to remove tag from key {}: {}", selectedKeyId, e.getMessage());
        }
    }

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