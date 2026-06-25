package eu.isygoit.ui.ims.views.tenant;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.TenantDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.TenantImageService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.tenant.dialog.DeleteTenantDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.TenantDetailsDialog;
import eu.isygoit.ui.ims.views.tenant.dialog.ToggleTenantStatusDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class TenantCard extends BaseCard<TenantManagementView, TenantService> {

    private final TenantDto tenant;
    private final TenantImageService tenantImageService;
    private final Runnable onRefresh;

    private Image tenantImage;
    private Span adminStatusChip;
    private Button toggleStatusBtn;

    public TenantCard(TenantManagementView parentView,
                      TenantService tenantService,
                      TenantImageService tenantImageService,
                      TenantDto tenant,
                      Runnable onRefresh) {
        super(parentView, tenantService);
        this.tenant = tenant;
        this.tenantImageService = tenantImageService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "tenant-card";
    }

    @Override
    protected Component buildTitle() {
        VerticalLayout titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.setWidthFull();

        // Row 1: image and action buttons
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        tenantImage = new Image();
        tenantImage.setWidth("48px");
        tenantImage.setHeight("48px");
        tenantImage.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
        tenantImage.setSrc(getSvgPlaceholder());

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        row1.add(tenantImage, buttonBar);
        row1.expand(buttonBar);

        // Row 2: name + status chip
        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.getStyle().set("flex-wrap", "wrap");
        row2.getStyle().set("margin-top", "var(--lumo-space-s)");

        Span titleSpan = buildTitleSpan(tenant.getName(), tenant.getEmail());
        adminStatusChip = buildStatusChip(
                tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : I18n.t("tenant.card.status.unknown"),
                tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : I18n.t("tenant.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip);

        titleLayout.add(row1, row2);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("tenant.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new TenantDetailsDialog(parentView, objectService, tenant.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("tenant.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateTenantDialog(tenant, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        toggleStatusBtn = createIconButton(
                tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("tenant.card.disable.tooltip") : I18n.t("tenant.card.enable.tooltip")
        );
        toggleStatusBtn.addClickListener(e -> openToggleStatusDialog());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("tenant.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteTenantDialog(parentView, objectService, tenant.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, toggleStatusBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.ENVELOPE, I18n.t("tenant.card.email"), tenant.getEmail()));
        body.add(createIconRow(VaadinIcon.PHONE, I18n.t("tenant.card.phone"), tenant.getPhone()));
        if (tenant.getIndustry() != null && !tenant.getIndustry().isBlank()) {
            body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("tenant.card.industry"), tenant.getIndustry()));
        }
        if (tenant.getDescription() != null && !tenant.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("tenant.card.description"), tenant.getDescription()));
        }
        if (tenant.getUrl() != null && !tenant.getUrl().isBlank()) {
            body.add(createIconRow(VaadinIcon.GLOBE, I18n.t("tenant.card.website"), tenant.getUrl()));
        }
        add(body);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        com.vaadin.flow.component.icon.Icon iconComponent = icon.create();
        iconComponent.setSize("14px");
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("word-break", "break-all");
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void openToggleStatusDialog() {
        new ToggleTenantStatusDialog(parentView, objectService, tenant.getId(), tenant.getAdminStatus(), () -> {
            refreshTenantData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshTenantData() {
        try {
            ResponseEntity<TenantDto> response = objectService.findById(tenant.getId());
            if (response.getBody() != null) {
                tenant.setAdminStatus(response.getBody().getAdminStatus());
                updateStatusChip();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh tenant data", e);
        }
    }

    private void updateStatusChip() {
        if (adminStatusChip != null) {
            String status = tenant.getAdminStatus() != null ? tenant.getAdminStatus().name() : I18n.t("tenant.card.status.unknown");
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            adminStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = tenant.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("tenant.card.disable.tooltip") : I18n.t("tenant.card.enable.tooltip"));
        }
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateStatusChip();
        loadTenantImage();
    }

    private void loadTenantImage() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<Resource> response = tenantImageService.downloadImage(tenant.getId());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] imageBytes = response.getBody().getContentAsByteArray();
                    if (imageBytes != null && imageBytes.length > 0) {
                        StreamResource resource = new StreamResource("tenant_" + tenant.getId() + ".jpg",
                                () -> new ByteArrayInputStream(imageBytes));
                        tenantImage.setSrc(resource);
                        return;
                    }
                }
            } catch (FeignException ex) {
                if (ex.status() == 404) {
                    log.debug("No image found for tenant {}", tenant.getId());
                } else {
                    log.warn("Failed to load image for tenant {}: {}", tenant.getId(), ex.getMessage());
                }
            } catch (IOException e) {
                log.error("Error reading image stream for tenant {}", tenant.getId(), e);
            }
            tenantImage.setSrc(getSvgPlaceholder());
        }));
    }

    private String getSvgPlaceholder() {
        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'%3E%3Crect x='2' y='2' width='20' height='20' rx='2.18' ry='2.18'%3E%3C/rect%3E%3Cpath d='M7 2v20M17 2v20M2 12h20M2 7h5M2 17h5M17 17h5M17 7h5'%3E%3C/path%3E%3C/svg%3E";
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .tenant-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .tenant-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .tenant-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .tenant-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .tenant-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .tenant-card .tenant-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}