package eu.isygoit.ui.ims.views.customer;

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
import eu.isygoit.dto.data.CustomerDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.CustomerImageService;
import eu.isygoit.remote.ims.CustomerService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.customer.dialog.CustomerDetailsDialog;
import eu.isygoit.ui.ims.views.customer.dialog.DeleteCustomerDialog;
import eu.isygoit.ui.ims.views.customer.dialog.LinkCustomerAccountDialog;
import eu.isygoit.ui.ims.views.customer.dialog.ToggleCustomerStatusDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class CustomerCard extends BaseCard<CustomerManagementView, CustomerService> {

    private final CustomerDto customer;
    private final CustomerImageService customerImageService;
    private final AccountService accountService;
    private final Runnable onRefresh;

    private Image customerImage;
    private Span adminStatusChip;
    private Button toggleStatusBtn;

    public CustomerCard(CustomerManagementView parentView,
                        CustomerService customerService,
                        CustomerImageService customerImageService,
                        AccountService accountService,
                        CustomerDto customer,
                        Runnable onRefresh) {
        super(parentView, customerService);
        this.customer = customer;
        this.customerImageService = customerImageService;
        this.accountService = accountService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "customer-card";
    }

    @Override
    protected Component buildTitle() {
        VerticalLayout titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.setWidthFull();

        // Row 1: avatar image only — action buttons live exclusively in the
        // card's footer (built once by BaseCard#buildFooter()).
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.addClassName("card-row");

        customerImage = new Image();
        customerImage.setWidth("48px");
        customerImage.setHeight("48px");
        customerImage.addClassName("card-avatar");
        customerImage.setSrc(getSvgPlaceholder());

        row1.add(customerImage);

        // Row 2: name + status chip
        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.addClassName("card-row");
        row2.addClassName("card-row--spaced");

        Span titleSpan = buildTitleSpan(customer.getName(), customer.getEmail());
        adminStatusChip = buildStatusChip(
                customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("ims.customer.card.status.unknown"),
                customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("ims.customer.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip);

        titleLayout.add(row1, row2);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.customer.card.details.tooltip"),
                () -> new CustomerDetailsDialog(parentView, objectService, customer.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.customer.card.edit.tooltip"),
                () -> parentView.openUpdateCustomerDialog(customer, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button linkAccountBtn = createIconButton(VaadinIcon.LINK, I18n.t("ims.customer.card.link.account.tooltip"));
        linkAccountBtn.addClickListener(e -> openLinkAccountDialog());

        toggleStatusBtn = createToggleButton(
                customer.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED,
                I18n.t("ims.customer.card.enable.tooltip"),
                I18n.t("ims.customer.card.disable.tooltip"),
                this::openToggleStatusDialog
        );

        Button deleteBtn = createDeleteButton(I18n.t("ims.customer.card.delete.tooltip"),
                () -> new DeleteCustomerDialog(parentView, objectService, customer.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, linkAccountBtn, toggleStatusBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("card-row--spaced");

        body.add(createIconRow(VaadinIcon.ENVELOPE, I18n.t("ims.customer.card.email"), customer.getEmail()));
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
        iconComponent.addClassName("meta-row-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.addClassName("meta-row-label");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.addClassName("meta-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    private void openToggleStatusDialog() {
        new ToggleCustomerStatusDialog(parentView, objectService, customer.getId(), customer.getAdminStatus(), () -> {
            refreshCustomerData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void openLinkAccountDialog() {
        new LinkCustomerAccountDialog(parentView, objectService, accountService, customer.getId(), () -> {
            refreshCustomerData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshCustomerData() {
        try {
            ResponseEntity<CustomerDto> response = objectService.findById(customer.getId());
            if (response.getBody() != null) {
                CustomerDto updated = response.getBody();
                customer.setAdminStatus(updated.getAdminStatus());
                customer.setAccountCode(updated.getAccountCode());
                updateStatusChip();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh customer data", e);
        }
    }

    private void updateStatusChip() {
        if (adminStatusChip != null) {
            String status = customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("ims.customer.card.status.unknown");
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            applyChipColor(adminStatusChip, ChipColor.fromStatus(status));
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = customer.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("ims.customer.card.disable.tooltip") : I18n.t("ims.customer.card.enable.tooltip"));
        }
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateStatusChip();
        loadCustomerImage();
    }

    private void loadCustomerImage() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                ResponseEntity<Resource> response = customerImageService.downloadImage(customer.getId());
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] imageBytes = response.getBody().getContentAsByteArray();
                    if (imageBytes != null && imageBytes.length > 0) {
                        StreamResource resource = new StreamResource("customer_" + customer.getId() + ".jpg",
                                () -> new ByteArrayInputStream(imageBytes));
                        customerImage.setSrc(resource);
                        return;
                    }
                }
            } catch (FeignException ex) {
                if (ex.status() == 404) {
                    log.debug("No image found for customer {}", customer.getId());
                } else {
                    log.warn("Failed to load image for customer {}: {}", customer.getId(), ex.getMessage());
                }
            } catch (IOException e) {
                log.error("Error reading image stream for customer {}", customer.getId(), e);
            }
            customerImage.setSrc(getSvgPlaceholder());
        }));
    }

    private String getSvgPlaceholder() {
        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='8' r='4'%3E%3C/circle%3E%3Cpath d='M5.5 20v-2a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5v2'%3E%3C/path%3E%3C/svg%3E";
    }
}