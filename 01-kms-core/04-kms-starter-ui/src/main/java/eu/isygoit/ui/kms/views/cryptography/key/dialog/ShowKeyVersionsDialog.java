package eu.isygoit.ui.kms.views.cryptography.key.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.enums.IEnumKeyStatus;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.component.RowCard;
import eu.isygoit.ui.common.component.RowCardList;
import eu.isygoit.ui.common.dialog.DialogLayout;
import eu.isygoit.ui.kms.views.common.KmsActionDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

public class ShowKeyVersionsDialog extends KmsActionDialog {

    private final KmsApiService kmsApiService;
    private final String keyId;
    private final String aliasOrId;
    private final RowCardList<KmsDtos.ListKeyVersionsResponse.KeyVersion> list = new RowCardList<>();
    private final ProgressBar loadingBar = new ProgressBar();

    public ShowKeyVersionsDialog(KmsApiService kmsApiService,
                                 String keyId,
                                 String aliasOrId) {
        super(I18n.t("kms.key.dialog.versions.title", aliasOrId));
        this.kmsApiService = kmsApiService;
        this.keyId = keyId;
        this.aliasOrId = aliasOrId;

        setOkButtonText(I18n.t("kms.key.dialog.versions.button.close"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_TERTIARY);
        DialogLayout.size(this, DialogLayout.WIDTH_L);
        addClassName("show-key-versions-dialog");

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

        list.setVisible(false);
        list.setWidthFull();
        list.emptyText(I18n.t("kms.key.dialog.versions.empty"));
        list.cardFactory(this::buildVersionCard);

        layout.add(list);
        add(layout);
    }

    private RowCard buildVersionCard(KmsDtos.ListKeyVersionsResponse.KeyVersion version) {
        RowCard card = RowCard.create()
                .title(version.getVersionId())
                .tag(KmsEnumTag.ofOrUnknown(version.getStatus(), null))
                .tag(KmsEnumTag.ofValue(version.getSigningAlgorithm(), "kms.enum"));
        if (version.getOrigin() != null) {
            card.tag(KmsEnumTag.of(version.getOrigin(), null));
        }
        if (version.getExpirationModel() != null) {
            card.tag(KmsEnumTag.of(version.getExpirationModel(), null));
        }
        card.fact(I18n.t("kms.key.dialog.versions.column.creation.date"),
                version.getCreateDate() != null ? DateHelper.formatToHumanReadable(version.getCreateDate()) : null);
        card.fact(I18n.t("kms.key.dialog.versions.column.deactivation.date"),
                version.getDeactivationDate() != null ? DateHelper.formatToHumanReadable(version.getDeactivationDate()) : null);
        card.fact(I18n.t("kms.key.dialog.versions.column.expiry.date"),
                version.getValidTo() != null ? DateHelper.formatToHumanReadable(version.getValidTo().toLocalDate()) : null);

        IEnumKeyStatus.Types status = version.getStatus();
        if (status == IEnumKeyStatus.Types.ENABLED) {
            card.dangerAction(VaadinIcon.BAN, I18n.t("kms.key.dialog.versions.disable.tooltip"),
                    () -> new DisableKeyVersionDialog(kmsApiService, keyId, version.getVersionId(), this::loadVersions).open());
        } else if (status == IEnumKeyStatus.Types.DISABLED) {
            card.action(VaadinIcon.CHECK_CIRCLE, I18n.t("kms.key.dialog.versions.enable.tooltip"),
                    () -> new EnableKeyVersionDialog(kmsApiService, keyId, version.getVersionId(), this::loadVersions).open());
        }
        // PENDING_DELETION or other: no action possible
        return card;
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
            list.emptyText(I18n.t("kms.key.dialog.versions.empty"));
            list.setItems(versions);
        } catch (Exception e) {
            list.emptyText(I18n.t("kms.key.dialog.versions.load.failed", e.getMessage()));
            list.setItems(new ArrayList<>());
            showError(I18n.t("kms.key.dialog.versions.load.error"));
        } finally {
            loadingBar.setVisible(false);
            list.setVisible(true);
        }
    }
}
