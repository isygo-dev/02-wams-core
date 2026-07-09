package eu.isygoit.ui.kms.views.cryptography.keyStore.dialog;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

/**
 * Read-only dialog showing every field of a
 * {@link KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore}, for use when
 * the compact {@code StoreCard} isn't enough (i.e. "Details" action).
 */
@CssImport("./styles/kms.css")
public class CustomKeyStoreDetailsDialog extends NoActionDialog {

    public CustomKeyStoreDetailsDialog(KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
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
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity
        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(identityGrid, VaadinIcon.KEY, I18n.t("kms.keystore.details.field.id"),
                store.getCustomKeyStoreId() != null ? String.valueOf(store.getCustomKeyStoreId()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("kms.keystore.details.field.name"), store.getName());
        addFieldToGrid(identityGrid, VaadinIcon.COG, I18n.t("kms.keystore.details.field.type"), store.getCustomKeyStoreType());
        addFieldToGrid(identityGrid, VaadinIcon.FLAG, I18n.t("kms.keystore.details.field.status"),
                store.getStatus() != null ? store.getStatus().meaning() : null);
        addFieldToGrid(identityGrid, VaadinIcon.CONNECT, I18n.t("kms.keystore.details.field.connection.state"), store.getConnectionState());
        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("kms.keystore.details.field.connection.id"), store.getConnectionId());
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.identity"), identityGrid));

        // CloudHSM
        Div cloudHsmGrid = new Div();
        cloudHsmGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(cloudHsmGrid, VaadinIcon.CLOUD, I18n.t("kms.keystore.details.field.cloudhsm.cluster"), store.getCloudHsmClusterId());
        addFieldToGrid(cloudHsmGrid, VaadinIcon.LOCK, I18n.t("kms.keystore.details.field.keystore.password"),
                store.getKeyStorePassword() != null ? I18n.t("kms.keystore.details.value.redacted") : null);
        addFieldToGrid(cloudHsmGrid, VaadinIcon.FILE_TEXT, I18n.t("kms.keystore.details.field.trust.anchor"), store.getTrustAnchorCertificate());
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.cloudhsm"), cloudHsmGrid));

        // External key store (XKS)
        Div xksGrid = new Div();
        xksGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(xksGrid, VaadinIcon.LINK, I18n.t("kms.keystore.details.field.xks.endpoint"), store.getXksProxyUriEndpoint());
        addFieldToGrid(xksGrid, VaadinIcon.ROAD, I18n.t("kms.keystore.details.field.xks.path"), store.getXksProxyUriPath());
        addFieldToGrid(xksGrid, VaadinIcon.LOCK, I18n.t("kms.keystore.details.field.xks.credential"),
                store.getXksProxyAuthenticationCredential() != null ? I18n.t("kms.keystore.details.value.redacted") : null);
        addFieldToGrid(xksGrid, VaadinIcon.CONNECT, I18n.t("kms.keystore.details.field.xks.connectivity"), store.getXksProxyConnectivity());
        addFieldToGrid(xksGrid, VaadinIcon.CODE, I18n.t("kms.keystore.details.field.type.specific.data"), store.getCustomKeyStoreTypeSpecificData());
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.xks"), xksGrid));

        // Health & connectivity
        Div healthGrid = new Div();
        healthGrid.addClassName("wams-card__detail-grid");
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
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.health"), healthGrid));

        // Connection settings
        Div settingsGrid = new Div();
        settingsGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(settingsGrid, VaadinIcon.TIMER, I18n.t("kms.keystore.details.field.timeout"),
                store.getConnectionTimeoutSeconds() != null ? store.getConnectionTimeoutSeconds() + "s" : null);
        addFieldToGrid(settingsGrid, VaadinIcon.SPARK_LINE, I18n.t("kms.keystore.details.field.health.interval"),
                store.getHealthCheckIntervalSeconds() != null ? store.getHealthCheckIntervalSeconds() + "s" : null);
        addFieldToGrid(settingsGrid, VaadinIcon.REFRESH, I18n.t("kms.keystore.details.field.auto.reconnect"),
                store.getAutoReconnect() != null
                        ? (store.getAutoReconnect() ? I18n.t("kms.keystore.card.on") : I18n.t("kms.keystore.card.off"))
                        : null);
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.settings"), settingsGrid));

        // Metadata & tags
        Div metaGrid = new Div();
        metaGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(metaGrid, VaadinIcon.TAGS, I18n.t("kms.keystore.details.field.metadata"), store.getMetadata());
        addFieldToGrid(metaGrid, VaadinIcon.TAGS, I18n.t("kms.keystore.details.field.tags"), store.getTags());
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.metadata"), metaGrid));

        // Audit
        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("kms.keystore.details.field.created"),
                store.getCreateDate() != null ? DateHelper.formatToHumanReadable(store.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("kms.keystore.details.field.updated"),
                store.getUpdateDate() != null ? DateHelper.formatToHumanReadable(store.getUpdateDate()) : null);
        mainLayout.add(createSection(I18n.t("kms.keystore.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;

        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, labelSpan);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        container.add(field);
    }

    private VerticalLayout createSection(String title, Div content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
    }
}
