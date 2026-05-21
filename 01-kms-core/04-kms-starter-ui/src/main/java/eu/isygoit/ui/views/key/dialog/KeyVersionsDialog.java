package eu.isygoit.ui.views.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.views.BaseActionDialog;
import org.springframework.http.ResponseEntity;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class KeyVersionsDialog extends BaseActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String aliasOrId;
    private final Grid<KmsDtos.ListKeyVersionsResponse.KeyVersion> grid = new Grid<>();
    private final ProgressBar loadingBar = new ProgressBar();

    public KeyVersionsDialog(KmsApiService kmsApiService, String keyId, String aliasOrId) {
        super("Key Versions: " + aliasOrId, null); // onSuccess not needed for read‑only dialog
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.aliasOrId = aliasOrId;

        setOkButtonText("Close");
        addThemeVariantsOkButton(ButtonVariant.LUMO_TERTIARY); // style ok button as tertiary
        setWidth("800px");
        setResizable(true);

        buildContent();
        loadVersions();
    }

    @Override
    protected boolean onOk() {
        // Simply close the dialog – no action needed
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
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getVersionId)
                .setHeader("Version ID").setSortable(true);
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getStatus)
                .setHeader("Status").setSortable(true);
        grid.addColumn(version -> version.getCreateDate() != null ?
                        version.getCreateDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "-")
                .setHeader("Creation Date").setSortable(true);
        grid.addColumn(KmsDtos.ListKeyVersionsResponse.KeyVersion::getSigningAlgorithm)
                .setHeader("Signing Algorithm");
        grid.addColumn(version -> version.getOrigin() != null ? version.getOrigin().name() : "-")
                .setHeader("Origin");
        layout.add(grid);

        add(layout);
    }

    private void loadVersions() {
        try {
            ResponseEntity<KmsDtos.ListKeyVersionsResponse> response =
                    kmsApiService.listKeyVersions(keyId, 100, null);
            if (response.getBody() != null && response.getBody().getVersions() != null) {
                List<KmsDtos.ListKeyVersionsResponse.KeyVersion> versions = response.getBody().getVersions();
                grid.setItems(versions);
                if (versions.isEmpty()) {
                    grid.setEmptyStateText("No versions found for this key.");
                }
            } else {
                grid.setItems(new ArrayList<>());
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