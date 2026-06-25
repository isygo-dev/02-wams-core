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
import eu.isygoit.ui.ims.views.account.dialog.AccountDetailsDialog;
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

        // Row 1: image and action buttons
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setWidthFull();
        row1.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row1.setAlignItems(FlexComponent.Alignment.CENTER);
        row1.setSpacing(true);
        row1.getStyle().set("flex-wrap", "wrap");

        accountImage = new Image();
        accountImage.setWidth("48px");
        accountImage.setHeight("48px");
        accountImage.getStyle()
                .set("border-radius", "50%")
                .set("object-fit", "cover")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border", "2px solid var(--lumo-contrast-20pct)");
        accountImage.setSrc(getSvgPlaceholder());

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        row1.add(accountImage, buttonBar);
        row1.expand(buttonBar);

        // Row 2: name/email + status chips
        HorizontalLayout row2 = new HorizontalLayout();
        row2.setAlignItems(FlexComponent.Alignment.CENTER);
        row2.setSpacing(true);
        row2.getStyle().set("flex-wrap", "wrap");
        row2.getStyle().set("margin-top", "var(--lumo-space-s)");

        String displayName = minAccount.getFullName() != null ? minAccount.getFullName() : minAccount.getEmail();
        Span titleSpan = buildTitleSpan(displayName, minAccount.getEmail());
        adminStatusChip = buildStatusChip(
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("account.card.status.unknown"),
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("account.card.status.unknown")
        );
        systemStatusChip = buildStatusChip(
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("account.card.status.unknown"),
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("account.card.status.unknown")
        );
        row2.add(titleSpan, adminStatusChip, systemStatusChip);

        titleLayout.add(row1, row2);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("account.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new AccountDetailsDialog(parentView, objectService, minAccount.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("account.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateAccountDialog(minAccount.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button resetPwdBtn = createIconButton(VaadinIcon.KEY, I18n.t("account.card.reset.password.tooltip"));
        resetPwdBtn.addClickListener(e -> parentView.openResetPasswordDialog(minAccount.getId(), minAccount.getEmail()));

        toggleStatusBtn = createIconButton(
                minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? I18n.t("account.card.disable.tooltip") : I18n.t("account.card.enable.tooltip")
        );
        toggleStatusBtn.addClickListener(e -> openToggleStatusDialog());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("account.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteAccountDialog(parentView, objectService, minAccount.getId(), () -> {
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
            String status = minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : I18n.t("account.card.status.unknown");
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            adminStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
        if (systemStatusChip != null) {
            String status = minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : I18n.t("account.card.status.unknown");
            systemStatusChip.setText(status);
            systemStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            systemStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
    }

    private void updateToggleButtonIcon() {
        if (toggleStatusBtn != null) {
            boolean enabled = minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED;
            toggleStatusBtn.setIcon(enabled ? VaadinIcon.LOCK.create() : VaadinIcon.UNLOCK.create());
            toggleStatusBtn.setTooltipText(enabled ? I18n.t("account.card.disable.tooltip") : I18n.t("account.card.enable.tooltip"));
        }
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createIconRow(VaadinIcon.MAILBOX, I18n.t("account.card.email"), minAccount.getEmail()));

        if (minAccount.getTenant() != null) {
            body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("account.card.tenant"), minAccount.getTenant()));
        }

        if (minAccount.getFullName() != null && !minAccount.getFullName().equals(minAccount.getEmail())) {
            body.add(createIconRow(VaadinIcon.USER, I18n.t("account.card.full.name"), minAccount.getFullName()));
        }

        if (minAccount.getFunctionRole() != null && !minAccount.getFunctionRole().isBlank()) {
            body.add(createIconRow(VaadinIcon.COG, I18n.t("account.card.function.role"), minAccount.getFunctionRole()));
        }

        String adminFlag = Boolean.TRUE.equals(minAccount.getIsAdmin()) ? I18n.t("account.card.yes") : I18n.t("account.card.no");
        body.add(createIconRow(VaadinIcon.SHIELD, I18n.t("account.card.admin"), adminFlag));

        if (minAccount.getAccountType() != null && !minAccount.getAccountType().isBlank()) {
            body.add(createIconRow(VaadinIcon.TAGS, I18n.t("account.card.account.type"), minAccount.getAccountType()));
        }

        if (minAccount.getLastConnectionDate() != null) {
            body.add(createIconRow(VaadinIcon.CLOCK, I18n.t("account.card.last.login"), minAccount.getLastConnectionDate().toString()));
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

    @Override
    protected String buildExtraStyles() {
        return """
                .account-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .account-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .account-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .account-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .account-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .account-card .account-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}