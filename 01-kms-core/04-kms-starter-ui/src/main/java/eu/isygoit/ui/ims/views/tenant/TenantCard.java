package eu.isygoit.ui.ims.views.tenant;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.tenant.dialog.DeleteTenantDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.TenantDetailsDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Slf4j
public class TenantCard extends BaseCard<TenantManagementView, TenantService> {

    private final TenantDto tenant;
    private Span adminStatusChip;

    public TenantCard(TenantManagementView parentView,
                      TenantService tenantService,
                      TenantDto tenant,
                      Runnable onRefresh) {
        super(parentView, tenantService);
        this.tenant = tenant;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "tenant-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(tenant.getName(), tenant.getEmail());
        adminStatusChip = buildStatusChip(
                tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : "UNKNOWN",
                tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : "UNKNOWN"
        );

        left.add(titleSpan, adminStatusChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full tenant details");
        detailsBtn.addClickListener(e -> new TenantDetailsDialog(parentView, objectService, tenant.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit tenant");
        editBtn.addClickListener(e -> parentView.openUpdateTenantDialog(tenant, this::refresh));

        Button toggleStatusBtn = createIconButton(
                tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable tenant" : "Enable tenant"
        );
        toggleStatusBtn.addClickListener(e -> toggleAdminStatus());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete tenant");
        deleteBtn.addClickListener(e -> new DeleteTenantDialog(parentView, objectService, tenant.getId(), this::refresh).open());

        return List.of(detailsBtn, editBtn, toggleStatusBtn, deleteBtn);
    }

    private void toggleAdminStatus() {
        parentView.showLoading(true);
        try {
            IEnumEnabledBinaryStatus.Types newStatus = tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED
                    ? IEnumEnabledBinaryStatus.Types.DISABLED
                    : IEnumEnabledBinaryStatus.Types.ENABLED;
            ResponseEntity<TenantDto> updated = objectService.updateAdminStatus(tenant.getId(), newStatus);
            tenant.setAdminStatus(updated.getBody().getAdminStatus());
            updateStatusChip();
            parentView.loadPageZero(); // refresh view
            parentView.showLoading(false);
        } catch (FeignException ex) {
            String errorMsg = (ex.status() == 500 || ex.status() == 400) ? ex.contentUTF8() : ex.getMessage();
            Notification.show("Failed to toggle tenant status: " + errorMsg, 6000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            log.error("Failed to toggle tenant status", ex);
        } catch (Exception e) {
            parentView.showLoading(false);
            log.error("Failed to toggle tenant status", e);
        }
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.ENVELOPE, "Email", tenant.getEmail()));
        add(createIconRow(VaadinIcon.PHONE, "Phone", tenant.getPhone()));
        if (tenant.getIndustry() != null && !tenant.getIndustry().isBlank()) {
            add(createIconRow(VaadinIcon.BUILDING, "Industry", tenant.getIndustry()));
        }
        if (tenant.getDescription() != null && !tenant.getDescription().isBlank()) {
            add(createIconRow(VaadinIcon.FILE_TEXT, "Description", tenant.getDescription()));
        }
        if (tenant.getUrl() != null && !tenant.getUrl().isBlank()) {
            add(createIconRow(VaadinIcon.GLOBE, "Website", tenant.getUrl()));
        }
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.getStyle().set("margin-top", "var(--lumo-space-xs)");
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XSMALL);
        labelSpan.getStyle().set("min-width", "100px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XSMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateStatusChip();
    }

    private void updateStatusChip() {
        if (adminStatusChip != null) {
            String status = tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : "UNKNOWN";
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            adminStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
    }

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                var response = objectService.findById(tenant.getId());
                if (response.getBody() != null) {
                    parentView.loadPageZero();
                }
            } catch (Exception e) {
                log.error("Failed to refresh tenant card", e);
            }
        }));
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .tenant-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .tenant-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .tenant-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .tenant-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}