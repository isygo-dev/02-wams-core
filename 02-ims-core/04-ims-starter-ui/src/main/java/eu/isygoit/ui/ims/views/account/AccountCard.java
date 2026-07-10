package eu.isygoit.ui.ims.views.account;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.account.dialog.AccountDetailsViewDialog;
import eu.isygoit.ui.ims.views.account.dialog.DeleteAccountDialog;
import eu.isygoit.ui.ims.views.account.dialog.EnableDisableAccountDialog;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
public class AccountCard extends BaseCard<AccountManagementView, AccountService> {

    private final MinAccountDto minAccount;
    private final AccountImageService accountImageService;
    private final Runnable onRefresh;

    private Image accountImage;
    private Span adminStatusChip;
    private Span systemStatusChip;
    private Button toggleStatusBtn;

    public AccountCard(AccountManagementView parentView,
                       AccountService accountService,
                       AccountImageService accountImageService,
                       MinAccountDto minAccount,
                       Runnable onRefresh) {
        super(parentView, accountService);
        this.minAccount = minAccount;
        this.accountImageService = accountImageService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "account-card";
    }

    @Override
    protected Component buildTitle() {
        VerticalLayout titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.setWidthFull();

        // Row 1: avatar image only — action buttons live exclusively in the
        // card's footer (built once by BaseCard#buildFooter()); this row used
        // to build its own duplicate button bar here, which both doubled the
        // buttons visually and left the top copies stale after a refresh.
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.addClassName("card-row");

        accountImage = new Image();
        accountImage.setWidth("48px");
        accountImage.setHeight("48px");
        accountImage.addClassName("card-avatar");
        accountImage.setSrc(getSvgPlaceholder());

        row1.add(accountImage);

        // Row 2: name/email + status chips
        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.addClassName("card-row");
        row2.addClassName("card-row--spaced");

        String displayName = minAccount.getFullName() != null ? minAccount.getFullName() : minAccount.getEmail();
        Span titleSpan = buildTitleSpan(displayName, minAccount.getEmail());
        adminStatusChip = buildStatusChip(
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("ims.account.card.status.unknown"),
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("ims.account.card.status.unknown")
        );
        systemStatusChip = buildStatusChip(
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("ims.account.card.status.unknown"),
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("ims.account.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip, systemStatusChip);

        titleLayout.add(row1, row2);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.account.card.details.tooltip"),
                () -> new AccountDetailsViewDialog(parentView, objectService, minAccount.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.account.card.edit.tooltip"),
                () -> parentView.openUpdateAccountDialog(minAccount.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button resetPwdBtn = createIconButton(VaadinIcon.KEY, I18n.t("ims.account.card.reset.password.tooltip"));
        resetPwdBtn.addClickListener(e -> parentView.openResetPasswordDialog(minAccount.getId(), minAccount.getEmail()));

        toggleStatusBtn = createToggleButton(
                minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED,
                I18n.t("ims.account.card.enable.tooltip"),
                I18n.t("ims.account.card.disable.tooltip"),
                this::openToggleStatusDialog
        );

        Button deleteBtn = createDeleteButton(I18n.t("ims.account.card.delete.tooltip"),
                () -> new DeleteAccountDialog(parentView, objectService, minAccount.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, resetPwdBtn, toggleStatusBtn, deleteBtn);
    }

    private void openToggleStatusDialog() {
        new EnableDisableAccountDialog(parentView, objectService, minAccount.getId(), () -> {
            refreshAccountData();
            if (onRefresh != null) onRefresh.run();
        }).open();
    }

    private void refreshAccountData() {
        try {
            ResponseEntity<AccountDto> response = objectService.findById(minAccount.getId());
            if (response.getBody() != null) {
                minAccount.setAdminStatus(response.getBody().getAdminStatus());
                minAccount.setSystemStatus(response.getBody().getSystemStatus());
                updateStatusChips();
                updateToggleButtonIcon();
            }
        } catch (Exception e) {
            log.error("Failed to refresh account data", e);
        }
    }

    private void updateStatusChips() {
        if (adminStatusChip != null) {
            String status = minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("ims.account.card.status.unknown");
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            applyChipColor(adminStatusChip, ChipColor.fromStatus(status));
        }
        if (systemStatusChip != null) {
            String status = minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("ims.account.card.status.unknown");
            systemStatusChip.setText(status);
            systemStatusChip.getElement().setAttribute("title", status);
            applyChipColor(systemStatusChip, ChipColor.fromStatus(status));
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("ims.account.card.disable.tooltip") : I18n.t("ims.account.card.enable.tooltip"));
        }
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("card-row--spaced");

        body.add(createIconRow(VaadinIcon.MAILBOX, I18n.t("ims.account.card.email"), minAccount.getEmail()));

        if (minAccount.getTenant() != null) {
            body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("ims.account.card.tenant"), minAccount.getTenant()));
        }

        add(body);
    }

    private HorizontalLayout createIconRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("meta-row");

        Icon iconComponent = icon.create();
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

    @Override
    protected void onCardAttach(AttachEvent event) {
        updateStatusChips();
        loadAccountImage();
    }

    private void loadAccountImage() {
        getUI().ifPresent(ui -> {
            ui.access(() -> {
                try {
                    ResponseEntity<Resource> response = accountImageService.downloadImage(minAccount.getId());
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        byte[] imageBytes = response.getBody().getContentAsByteArray();
                        if (imageBytes != null && imageBytes.length > 0) {
                            StreamResource resource = new StreamResource("account_" + minAccount.getId() + ".jpg",
                                    () -> new ByteArrayInputStream(imageBytes));
                            accountImage.setSrc(resource);
                            return;
                        }
                    }
                } catch (FeignException | IOException e) {
                    log.warn("Failed to load image", e);
                }
                accountImage.setSrc(getSvgPlaceholder());
            });
        });
    }

    private String getSvgPlaceholder() {
        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='48' height='48' viewBox='0 0 24 24' fill='none' stroke='%23888' stroke-width='1' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='12' cy='8' r='4'%3E%3C/circle%3E%3Cpath d='M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2'%3E%3C/path%3E%3C/svg%3E";
    }
}