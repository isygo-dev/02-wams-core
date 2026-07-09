package eu.isygoit.ui.kms.views.cryptography.keyStore;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.KmsDtos;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.kms.KmsApiService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.kms.views.cryptography.keyStore.dialog.CustomKeyStoreDetailsDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.dialog.DeleteCustomKeyStoreDialog;
import eu.isygoit.ui.kms.views.cryptography.keyStore.dialog.UpdateCustomKeyStoreDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
public class StoreCard extends BaseCard<CustomKeyStoresView, KmsApiService> {

    private final KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store;
    private final Long storeId;
    private final String storeName;
    private final String storeType;
    private final String storeStatus;
    private final String connectionState;
    private final String creationDate;
    private final String errorCode;

    // ── Constructor ───────────────────────────────────────────────────────────

    public StoreCard(CustomKeyStoresView parentView,
                     KmsApiService kmsApiService,
                     KmsDtos.DescribeCustomKeyStoreResponse.CustomKeyStore store) {
        super(parentView, kmsApiService);
        this.store = store;
        this.storeId = store.getCustomKeyStoreId();
        this.storeName = store.getName();
        this.storeType = store.getCustomKeyStoreType();
        this.storeStatus = store.getStatus() != null ? store.getStatus().name() : "UNKNOWN";
        this.connectionState = store.getConnectionState();
        this.creationDate = store.getCreateDate() != null
                ? DateHelper.formatToHumanReadable(store.getCreateDate()) : "-";
        this.errorCode = store.getConnectionError();
        initCard();
    }

    // ── Overrides ─────────────────────────────────────────────────────────────

    @Override
    protected String cardCssClassName() {
        return "store-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.addClassName("kms-partc-title-wrap");

        Span titleSpan = buildTitleSpan(storeName, storeName);
        Span typeChip = buildStatusChip(storeType, ChipColor.INFO);
        String statusDisplay = connectionState != null ? connectionState : storeStatus;
        Span statusChip = buildStatusChip(statusDisplay, storeStatus);

        left.add(titleSpan, typeChip, statusChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        // Standard contract: Details -> Edit -> entity-specific extras -> Delete (last, danger)
        Button detailsBtn = createDetailsButton(I18n.t("kms.keystore.card.details"), this::showDetails);

        Button updateBtn = createEditButton(I18n.t("kms.keystore.card.update"), this::updateCustomKeyStore);

        Button connectBtn = createIconButton(VaadinIcon.CONNECT, I18n.t("kms.keystore.card.connect"));
        connectBtn.addClickListener(e -> confirmConnect());
        connectBtn.setEnabled(!"CONNECTED".equalsIgnoreCase(connectionState));

        Button disconnectBtn = createIconButton(VaadinIcon.OUT, I18n.t("kms.keystore.card.disconnect"));
        disconnectBtn.addClickListener(e -> confirmDisconnect());
        disconnectBtn.setEnabled("CONNECTED".equalsIgnoreCase(connectionState));

        Button deleteBtn = createDeleteButton(I18n.t("kms.keystore.card.delete"), this::confirmDelete);

        return List.of(detailsBtn, updateBtn, connectBtn, disconnectBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        // Created (kept as the one always-present timestamp for quick scanning)
        add(createIconRow(VaadinIcon.CALENDAR, I18n.t("kms.keystore.card.created"), creationDate));

        // Health status / connection error — a broken connection must stay visible on the card
        if (StringUtils.hasText(errorCode)) {
            add(createIconRow(VaadinIcon.EXCLAMATION_CIRCLE, I18n.t("kms.keystore.card.error"), errorCode));
        } else if (StringUtils.hasText(store.getHealthStatus())) {
            add(createIconRow(VaadinIcon.HEART, I18n.t("kms.keystore.card.health"), store.getHealthStatus()));
        }
    }

    // ── Helper row builders ──────────────────────────────────────────────────

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("kms-partc-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.addClassName("kms-partc-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.addClassName("kms-partc-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    // ── Action handlers ──────────────────────────────────────────────────────

    private void showDetails() {
        new CustomKeyStoreDetailsDialog(store).open();
    }

    private void confirmConnect() {
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader(I18n.t("kms.keystore.connect.confirm.title"));
        dlg.setText(I18n.t("kms.keystore.connect.confirm.message", storeName));
        dlg.setCancelable(true);
        dlg.setConfirmText(I18n.t("kms.keystore.connect.confirm.button"));
        dlg.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName());
        dlg.addConfirmListener(e -> connectStore());
        dlg.open();
    }

    private void confirmDisconnect() {
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader(I18n.t("kms.keystore.disconnect.confirm.title"));
        dlg.setText(I18n.t("kms.keystore.disconnect.confirm.message"));
        dlg.setCancelable(true);
        dlg.setConfirmText(I18n.t("kms.keystore.disconnect.confirm.button"));
        dlg.setConfirmButtonTheme(ButtonVariant.LUMO_WARNING.getVariantName());
        dlg.addConfirmListener(e -> disconnectStore());
        dlg.open();
    }

    private void connectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.ConnectCustomKeyStoreResponse> resp =
                    objectService.connectCustomKeyStore(storeId);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            notify(ok ? I18n.t("kms.keystore.connect.success") : I18n.t("kms.keystore.connect.failed"), ok);
            if (ok) parentView.loadStores();
        } catch (FeignException ex) {
            String msg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            notify(I18n.t("kms.keystore.action.error", msg), false);
            log.error("Failed to connect store {}", storeId, ex);
        } catch (Exception ex) {
            notify(I18n.t("kms.keystore.action.error", ex.getMessage()), false);
            log.error("Failed to connect store {}", storeId, ex);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void disconnectStore() {
        parentView.showLoading(true);
        try {
            ResponseEntity<KmsDtos.DisconnectCustomKeyStoreResponse> resp =
                    objectService.disconnectCustomKeyStore(storeId);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            notify(ok ? I18n.t("kms.keystore.disconnect.success") : I18n.t("kms.keystore.disconnect.failed"), ok);
            if (ok) parentView.loadStores();
        } catch (FeignException ex) {
            String msg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            notify(I18n.t("kms.keystore.action.error", msg), false);
            log.error("Failed to disconnect store {}", storeId, ex);
        } catch (Exception ex) {
            notify(I18n.t("kms.keystore.action.error", ex.getMessage()), false);
            log.error("Failed to disconnect store {}", storeId, ex);
        } finally {
            parentView.showLoading(false);
        }
    }

    private void updateCustomKeyStore() {
        new UpdateCustomKeyStoreDialog(parentView, objectService, parentView::loadStores, store).open();
    }

    private void confirmDelete() {
        new DeleteCustomKeyStoreDialog(parentView, objectService, parentView::loadStores, store).open();
    }

    private void notify(String msg, boolean success) {
        Notification notification = Notification.show(msg, 6000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(
                success ? NotificationVariant.LUMO_SUCCESS : NotificationVariant.LUMO_ERROR
        );
    }
}