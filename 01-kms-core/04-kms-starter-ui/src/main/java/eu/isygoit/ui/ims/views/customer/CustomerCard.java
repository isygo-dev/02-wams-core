package eu.isygoit.ui.ims.views.customer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
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
    protected Component buildTitle() {
        return new Div();
    }

    @Override
    protected String cardCssClassName() {
        return "customer-card";
    }

    @Override
    protected void buildHeader() {
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        customerImage = new Image();
        customerImage.setWidth("48px");
        customerImage.setHeight("48px");
        customerImage.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
        customerImage.setSrc(getSvgPlaceholder());

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        row1.add(customerImage, buttonBar);
        row1.expand(buttonBar);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.getStyle().set("flex-wrap", "wrap");
        row2.getStyle().set("margin-top", "var(--lumo-space-s)");

        Span titleSpan = buildTitleSpan(customer.getName(), customer.getEmail());
        adminStatusChip = buildStatusChip(
                customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("customer.card.status.unknown"),
                customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("customer.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip);

        add(row1, row2);
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("customer.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new CustomerDetailsDialog(parentView, objectService, customer.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("customer.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateCustomerDialog(customer, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        toggleStatusBtn = createIconButton(
                customer.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                customer.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("customer.card.disable.tooltip") : I18n.t("customer.card.enable.tooltip")
        );
        toggleStatusBtn.addClickListener(e -> openToggleStatusDialog());

        Button linkAccountBtn = createIconButton(VaadinIcon.LINK, I18n.t("customer.card.link.account.tooltip"));
        linkAccountBtn.addClickListener(e -> openLinkAccountDialog());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("customer.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteCustomerDialog(parentView, objectService, customer.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, toggleStatusBtn, linkAccountBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.ENVELOPE, I18n.t("customer.card.email"), customer.getEmail()));
        body.add(createIconRow(VaadinIcon.PHONE, I18n.t("customer.card.phone"), customer.getPhoneNumber()));
        if (customer.getAccountCode() != null && !customer.getAccountCode().isBlank()) {
            body.add(createIconRow(VaadinIcon.KEY, I18n.t("customer.card.account.code"), customer.getAccountCode()));
        }
        if (customer.getUrl() != null && !customer.getUrl().isBlank()) {
            body.add(createIconRow(VaadinIcon.GLOBE, I18n.t("customer.card.website"), customer.getUrl()));
        }
        if (customer.getDescription() != null && !customer.getDescription().isBlank()) {
            body.add(createIconRow(VaadinIcon.FILE_TEXT, I18n.t("customer.card.description"), customer.getDescription()));
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
            String status = customer.getAdminStatus() != null ? customer.getAdminStatus().name() : I18n.t("customer.card.status.unknown");
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
            boolean enabled = customer.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("customer.card.disable.tooltip") : I18n.t("customer.card.enable.tooltip"));
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
                        return; // Success, skip fallback
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
            // Fallback to placeholder
            customerImage.setSrc(getSvgPlaceholder());
        }));
    }

    private String getSvgPlaceholder() {
        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='8' r='4'%3E%3C/circle%3E%3Cpath d='M5.5 20v-2a5 5 0 0 1 5-5h3a5 5 0 0 1 5 5v2'%3E%3C/path%3E%3C/svg%3E";
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .customer-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .customer-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .customer-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .customer-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .customer-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .customer-card .customer-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}