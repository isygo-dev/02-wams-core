package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.kms.views.common.KmsDetailsDialog;
import eu.isygoit.ui.kms.views.common.KmsEnumTag;

/**
 * Read-only dialog showing every field of a
 * {@link KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore}, for use when
 * the compact {@code StoreCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class CustomKeyStoreDetailsViewDialog extends KmsDetailsDialog {

    public CustomKeyStoreDetailsViewDialog(KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(I18n.t("kms.keystore.details.title"));

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("custom-keystore-details-dialog");

        buildContent(store);
    }

    private void buildContent(KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        // Identity
        Div identityGrid = createDetailGrid();
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.keystore.details.field.id"),
                store.getCustomKeyStoreId() != null ? String.valueOf(store.getCustomKeyStoreId()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.keystore.details.field.name"), store.getName());
        identityGrid.add(KmsEnumTag.detailField(VaadinIcon.COG, I18n.t("kms.keystore.details.field.type"),
                KmsEnumTag.ofValue(store.getCustomKeyStoreType(), "kms.enum")));
        identityGrid.add(KmsEnumTag.detailField(VaadinIcon.FLAG, I18n.t("kms.keystore.details.field.status"),
                KmsEnumTag.ofOrUnknown(store.getStatus(), "kms.enum")));
        identityGrid.add(KmsEnumTag.detailField(VaadinIcon.CONNECT, I18n.t("kms.keystore.details.field.connection.state"),
                KmsEnumTag.ofValue(store.getConnectionState(), "kms.enum")));
        // Connection id is a stable identifier worth copying even though it may be short.
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("kms.keystore.details.field.connection.id"), store.getConnectionId(), true);
        addTab(I18n.t("kms.keystore.details.section.identity"),
                createSection(I18n.t("kms.keystore.details.section.identity"), identityGrid));

        // CloudHSM
        Div cloudHsmGrid = createDetailGrid();
        // Cluster id is an AWS resource identifier worth copying even if short.
        addFieldToGrid(cloudHsmGrid, VaadinIcon.CLOUD, I18n.t("kms.keystore.details.field.cloudhsm.cluster"), store.getCloudHsmClusterId(), true);
        addFieldToGrid(cloudHsmGrid, VaadinIcon.LOCK, I18n.t("kms.keystore.details.field.keystore.password"),
                store.getKeyStorePassword() != null ? I18n.t("kms.keystore.details.value.redacted") : null);
        // Trust anchor certificate is PEM material — always copyable.
        addFieldToGrid(cloudHsmGrid, VaadinIcon.FILE_TEXT, I18n.t("kms.keystore.details.field.trust.anchor"), store.getTrustAnchorCertificate(), true);
        addTab(I18n.t("kms.keystore.details.section.cloudhsm"),
                createSection(I18n.t("kms.keystore.details.section.cloudhsm"), cloudHsmGrid));

        // External key store (XKS)
        Div xksGrid = createDetailGrid();
        // Endpoint/path are URLs worth copying even if short.
        addFieldToGrid(xksGrid, VaadinIcon.LINK, I18n.t("kms.keystore.details.field.xks.endpoint"), store.getXksProxyUriEndpoint(), true);
        addFieldToGrid(xksGrid, VaadinIcon.ROAD, I18n.t("kms.keystore.details.field.xks.path"), store.getXksProxyUriPath(), true);
        addFieldToGrid(xksGrid, VaadinIcon.LOCK, I18n.t("kms.keystore.details.field.xks.credential"),
                store.getXksProxyAuthenticationCredential() != null ? I18n.t("kms.keystore.details.value.redacted") : null);
        xksGrid.add(KmsEnumTag.detailField(VaadinIcon.CONNECT,
                I18n.t("kms.keystore.details.field.xks.connectivity"),
                KmsEnumTag.ofValue(store.getXksProxyConnectivity(), "kms.enum")));
        addFieldToGrid(xksGrid, VaadinIcon.CODE, I18n.t("kms.keystore.details.field.type.specific.data"), store.getCustomKeyStoreTypeSpecificData());
        addTab(I18n.t("kms.keystore.details.section.xks"),
                createSection(I18n.t("kms.keystore.details.section.xks"), xksGrid));

        // Health & connectivity
        Div healthGrid = createDetailGrid();
        addFieldToGrid(healthGrid, VaadinIcon.HEART, I18n.t("kms.keystore.details.field.health"), store.getHealthStatus());
        addFieldToGrid(healthGrid, VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.keystore.details.field.error"), store.getConnectionError());
        addFieldToGrid(healthGrid, VaadinIcon.KEY, I18n.t("kms.keystore.details.field.max.keys"),
                store.getMaxKeys() != null ? String.valueOf(store.getMaxKeys()) : null);
        addFieldToGrid(healthGrid, VaadinIcon.CONNECT, I18n.t("kms.keystore.details.field.last.connected"),
                store.getLastSuccessfulConnection() != null ? DateHelper.formatToHumanReadable(store.getLastSuccessfulConnection()) : null);
        addFieldToGrid(healthGrid, VaadinIcon.CLOCK, I18n.t("kms.keystore.details.field.last.attempt"),
                store.getLastConnectionAttempt() != null ? DateHelper.formatToHumanReadable(store.getLastConnectionAttempt()) : null);
        addFieldToGrid(healthGrid, VaadinIcon.STOPWATCH, I18n.t("kms.keystore.details.field.last.health.check"),
                store.getLastHealthCheck() != null ? DateHelper.formatToHumanReadable(store.getLastHealthCheck()) : null);
        addTab(I18n.t("kms.keystore.details.section.health"),
                createSection(I18n.t("kms.keystore.details.section.health"), healthGrid));

        // Connection settings
        Div settingsGrid = createDetailGrid();
        addFieldToGrid(settingsGrid, VaadinIcon.TIMER, I18n.t("kms.keystore.details.field.timeout"),
                store.getConnectionTimeoutSeconds() != null ? store.getConnectionTimeoutSeconds() + "s" : null);
        addFieldToGrid(settingsGrid, VaadinIcon.SPARK_LINE, I18n.t("kms.keystore.details.field.health.interval"),
                store.getHealthCheckIntervalSeconds() != null ? store.getHealthCheckIntervalSeconds() + "s" : null);
        addFieldToGrid(settingsGrid, VaadinIcon.REFRESH, I18n.t("kms.keystore.details.field.auto.reconnect"),
                store.getAutoReconnect() != null
                        ? (store.getAutoReconnect() ? I18n.t("kms.keystore.card.on") : I18n.t("kms.keystore.card.off"))
                        : null);
        addTab(I18n.t("kms.keystore.details.section.settings"),
                createSection(I18n.t("kms.keystore.details.section.settings"), settingsGrid));

        // Metadata & tags
        Div metaGrid = createDetailGrid();
        addFieldToGrid(metaGrid, VaadinIcon.TAGS, I18n.t("kms.keystore.details.field.metadata"), store.getMetadata());
        addFieldToGrid(metaGrid, VaadinIcon.TAGS, I18n.t("kms.keystore.details.field.tags"), store.getTags());
        addTab(I18n.t("kms.keystore.details.section.metadata"),
                createSection(I18n.t("kms.keystore.details.section.metadata"), metaGrid));

        // Audit
        Div auditGrid = createDetailGrid();
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.keystore.details.field.created"),
                store.getCreateDate() != null ? DateHelper.formatToHumanReadable(store.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.keystore.details.field.updated"),
                store.getUpdateDate() != null ? DateHelper.formatToHumanReadable(store.getUpdateDate()) : null);
        addTab(I18n.t("kms.keystore.details.section.audit"),
                createSection(I18n.t("kms.keystore.details.section.audit"), auditGrid));
    }
}
