package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KeyVersionsDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String aliasOrId;
    private final Grid<KmsDtos.ListKeyVersionsResponse.KeyVersion> grid = new Grid<>();
    private final ProgressBar loadingBar = new ProgressBar();

    public KeyVersionsDialog(KmsApiService kmsApiService, String keyId, String aliasOrId) {
        super("Key Versions: " + aliasOrId, null);
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.aliasOrId = aliasOrId;

        setOkButtonText("Close");
        addThemeVariantsOkButton(ButtonVariant.LUMO_TERTIARY);
        setWidth("90%");
        setMaxWidth("1200px");
        setResizable(true);

        buildContent();
        loadVersions();
    }

    @Override
    protected boolean onOk() {
        close();
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
                .setHeader("Version ID").setSortable(true).setResizable(true);

        // Status column with colored chip
        grid.addColumn(new ComponentRenderer<>(version -> {
            String status = version.getStatus() != null ? version.getStatus().name() : "UNKNOWN";
            Span chip = new Span(status);
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
                    break;
                case "DISABLED":
                    chip.getStyle().set("background-color", "#FEF3F2").set("color", "#C73A2B");
                    break;
                case "PENDING_DELETION":
                    chip.getStyle().set("background-color", "#FFF4E5").set("color", "#B25600");
                    break;
                default:
                    chip.getStyle().set("background-color", "#F2F4F8").set("color", "#5E6C84");
            }
            return chip;
        })).setHeader("Status").setSortable(true).setResizable(true);

        // Creation date column
        grid.addColumn(version -> version.getCreateDate() != null ?
                        version.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-")
                .setHeader("Creation Date").setSortable(true).setResizable(true);

        // Signing algorithm column
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getSigningAlgorithm)
                .setHeader("Signing Algorithm").setResizable(true);

        // Origin column
        grid.addColumn(version -> version.getOrigin() != null ? version.getOrigin().name() : "-")
                .setHeader("Origin").setSortable(true).setResizable(true);

        // Deactivation date column
        grid.addColumn(version -> version.getDeactivationDate() != null ?
                        version.getDeactivationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-")
                .setHeader("Deactivation Date").setResizable(true);

        // Expiry date column
        grid.addColumn(version -> version.getValidTo() != null ?
                        version.getValidTo().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-")
                .setHeader("Expiry Date").setResizable(true);

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
            // Sort: active (ENABLED) first, then by creation date descending
            versions.sort((v1, v2) -> {
                boolean isActive1 = v1.getStatus() == IEnumKeyStatus.Types.ENABLED;
                boolean isActive2 = v2.getStatus() == IEnumKeyStatus.Types.ENABLED;
                if (isActive1 && !isActive2) return -1;
                if (!isActive1 && isActive2) return 1;
                // If both active or both not, sort by creation date descending
                if (v1.getCreateDate() == null && v2.getCreateDate() == null) return 0;
                if (v1.getCreateDate() == null) return 1;
                if (v2.getCreateDate() == null) return -1;
                return v2.getCreateDate().compareTo(v1.getCreateDate());
            });
            grid.setItems(versions);
            if (versions.isEmpty()) {
                grid.setEmptyStateText("No versions found for this key.");
            }
        } catch (Exception e) {
            grid.setItems(new ArrayList<>());
            grid.setEmptyStateText("Failed to load versions: " + e.getMessage());
            showError("Could not retrieve key versions. Check server logs.");
        } finally {
            loadingBar.setVisible(false);
            grid.setVisible(true);
        }
    }
}