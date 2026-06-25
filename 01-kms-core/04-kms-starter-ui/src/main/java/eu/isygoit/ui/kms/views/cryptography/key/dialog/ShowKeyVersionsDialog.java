package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.dialog.BaseActionDialog;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class ShowKeyVersionsDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String aliasOrId;
    private final Grid<KmsDtos.ListKeyVersionsResponse.KeyVersion> grid = new Grid<>();
    private final ProgressBar loadingBar = new ProgressBar();

    public ShowKeyVersionsDialog(KmsApiService kmsApiService,
                                 String keyId,
                                 String aliasOrId) {
        super(I18n.t("key.dialog.versions.title", aliasOrId));
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.aliasOrId = aliasOrId;

        setOkButtonText(I18n.t("key.dialog.versions.button.close"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_TERTIARY);
        setWidth("90%");
        setMaxWidth("1200px");
        setResizable(true);

        buildContent();
        loadVersions();
    }

    @Override
    protected boolean onOk() {

        return true;
    }

    private void buildContent() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(true);
        layout.add(loadingBar);

        grid.setVisible(false);
        grid.setWidthFull();
        grid.setColumnReorderingAllowed(true);
        grid.setHeight("400px");

        // Version ID column
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getVersionId)
                .setHeader(I18n.t("key.dialog.versions.column.version.id")).setSortable(true).setResizable(true);

        // Status column with colored chip
        grid.addColumn(new ComponentRenderer<>(version -> {
            String status = version.getStatus() != null ? version.getStatus().name() : "UNKNOWN";
            String displayStatus;
            Span chip = new Span();
            chip.addClassName("status-chip");
            chip.getStyle()
                    .set("display", "inline-block")
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "var(--lumo-border-radius-m)")
                    .set("font-size", "var(--lumo-font-size-xs)")
                    .set("font-weight", "500");
            switch (status) {
                case "ENABLED":
                    chip.getStyle().set("background-color", "#E3F7E5").set("color", "#1E7B2E");
                    displayStatus = I18n.t("key.dialog.versions.status.enabled");
                    break;
                case "DISABLED":
                    chip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
                    displayStatus = I18n.t("key.dialog.versions.status.disabled");
                    break;
                case "PENDING_DELETION":
                    chip.getStyle().set("background-color", "#FFF4E5").set("color", "#B25600");
                    displayStatus = I18n.t("key.dialog.versions.status.pending.deletion");
                    break;
                default:
                    chip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
                    displayStatus = I18n.t("key.dialog.versions.status.unknown");
            }
            chip.setText(displayStatus);
            return chip;
        })).setHeader(I18n.t("key.dialog.versions.column.status")).setSortable(true).setResizable(true);

        // Creation date column – simple string formatting, sortable
        grid.addColumn(version -> version.getCreateDate() != null ?
                        DateHelper.formatToHumanReadable(version.getCreateDate()) : "-")
                .setHeader(I18n.t("key.dialog.versions.column.creation.date"))
                .setSortable(true)
                .setResizable(true);

        // Signing algorithm column
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getSigningAlgorithm)
                .setHeader(I18n.t("key.dialog.versions.column.signing.algorithm")).setResizable(true);

        // Origin column
        grid.addColumn(version -> version.getOrigin() != null ? version.getOrigin().name() : "-")
                .setHeader(I18n.t("key.dialog.versions.column.origin")).setSortable(true).setResizable(true);

        // Deactivation date column
        grid.addColumn(version -> version.getDeactivationDate() != null ?
                        DateHelper.formatToHumanReadable(version.getDeactivationDate()) : "-")
                .setHeader(I18n.t("key.dialog.versions.column.deactivation.date")).setResizable(true);

        // Expiry date column
        grid.addColumn(version -> version.getValidTo() != null ?
                        DateHelper.formatToHumanReadable(version.getValidTo().toLocalDate()) : "-")
                .setHeader(I18n.t("key.dialog.versions.column.expiry.date")).setResizable(true);

        // --- ACTIONS COLUMN with both Enable/Disable icon buttons ---
        grid.addColumn(new ComponentRenderer<>(version -> {
            IEnumKeyStatus.Types status = version.getStatus();
            Button actionBtn = new Button();

            if (status == IEnumKeyStatus.Types.ENABLED) {
                // Disable button
                actionBtn.setIcon(VaadinIcon.BAN.create());
                actionBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                actionBtn.setTooltipText(I18n.t("key.dialog.versions.disable.tooltip"));
                actionBtn.addClickListener(e -> {
                    DisableKeyVersionDialog dialog = new DisableKeyVersionDialog(
                            kmsApiService, keyId, version.getVersionId(), this::loadVersions);
                    dialog.open();
                });
            } else if (status == IEnumKeyStatus.Types.DISABLED) {
                // Enable button
                actionBtn.setIcon(VaadinIcon.CHECK_CIRCLE.create());
                actionBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
                actionBtn.setTooltipText(I18n.t("key.dialog.versions.enable.tooltip"));
                actionBtn.addClickListener(e -> {
                    EnableKeyVersionDialog dialog = new EnableKeyVersionDialog(
                            kmsApiService, keyId, version.getVersionId(), this::loadVersions);
                    dialog.open();
                });
            } else {
                // PENDING_DELETION or other – no action possible
                actionBtn.setIcon(VaadinIcon.MINUS_CIRCLE.create());
                actionBtn.setEnabled(false);
                actionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                actionBtn.setTooltipText(I18n.t("key.dialog.versions.cannot.change.tooltip"));
            }
            return actionBtn;
        })).setHeader(I18n.t("key.dialog.versions.column.actions")).setResizable(false).setWidth("80px").setFlexGrow(0);

        layout.add(grid);
        add(layout);
    }

    private void loadVersions() {
        try {
            ResponseEntity<KmsDtos.ListKeyVersionsResponse> response =
                    kmsApiService.listKeyVersions(keyId, 100, null);
            List<KmsDtos.ListKeyVersionsResponse.KeyVersion> versions = new ArrayList<>();
            if (response.getBody() != null && response.getBody().getVersions() != null) {
                versions = response.getBody().getVersions();
            }
            // Default sort: newest first by creation date
            versions.sort((v1, v2) -> {
                if (v1.getCreateDate() == null && v2.getCreateDate() == null) return 0;
                if (v1.getCreateDate() == null) return 1;
                if (v2.getCreateDate() == null) return -1;
                return v2.getCreateDate().compareTo(v1.getCreateDate());
            });
            grid.setItems(versions);
            if (versions.isEmpty()) {
                grid.setEmptyStateText(I18n.t("key.dialog.versions.empty"));
            }
        } catch (Exception e) {
            grid.setItems(new ArrayList<>());
            grid.setEmptyStateText(I18n.t("key.dialog.versions.load.failed", e.getMessage()));
            showError(I18n.t("key.dialog.versions.load.error"));
        } finally {
            loadingBar.setVisible(false);
            grid.setVisible(true);
        }
    }
}