package eu.isygoit.ui.ims.views.account;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.account.dialog.AccountDetailsDialog;
import eu.isygoit.ui.ims.views.account.dialog.DeleteAccountDialog;
import eu.isygoit.ui.ims.views.account.dialog.EnableDisableAccountDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AccountCard extends BaseCard<AccountManagementView, AccountService> {

    private final MinAccountDto minAccount;
    private Span adminStatusChip;
    private Span systemStatusChip;

    public AccountCard(AccountManagementView parentView,
                       AccountService accountService,
                       MinAccountDto minAccount) {
        super(parentView, accountService);
        this.minAccount = minAccount;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "account-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout left = new HorizontalLayout();
        left.setAlignItems(FlexComponent.Alignment.CENTER);
        left.setSpacing(true);
        left.getStyle().set("flex-wrap", "wrap");

        String displayName = minAccount.getFullName() != null ? minAccount.getFullName() : minAccount.getEmail();
        Span titleSpan = buildTitleSpan(displayName, minAccount.getEmail());
        adminStatusChip = buildStatusChip(
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : "UNKNOWN",
                minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : "UNKNOWN"
        );
        systemStatusChip = buildStatusChip(
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : "UNKNOWN",
                minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : "UNKNOWN"
        );

        left.add(titleSpan, adminStatusChip, systemStatusChip);
        return left;
    }

    @Override
    protected List<Button> buildActionButtons() {
        // New details button
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View full account details");
        detailsBtn.addClickListener(e -> new AccountDetailsDialog(parentView, objectService, minAccount.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit account details");
        editBtn.addClickListener(e -> parentView.openUpdateAccountDialog(minAccount.getId(), this::refresh));

        Button resetPwdBtn = createIconButton(VaadinIcon.KEY, "Reset password");
        resetPwdBtn.addClickListener(e -> parentView.openResetPasswordDialog(minAccount.getId(), minAccount.getEmail()));

        Button toggleStatusBtn = createIconButton(
                minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? VaadinIcon.LOCK : VaadinIcon.UNLOCK,
                minAccount.getAdminStatus() == IEnumEnabledBinaryStatus.Types.ENABLED ? "Disable account" : "Enable account"
        );
        toggleStatusBtn.addClickListener(e -> new EnableDisableAccountDialog(parentView, objectService, minAccount.getId(), this::refresh).open());

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete account");
        deleteBtn.addClickListener(e -> new DeleteAccountDialog(parentView, objectService, minAccount.getId(), this::refresh).open());

        return List.of(detailsBtn, editBtn, resetPwdBtn, toggleStatusBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        add(createIconRow(VaadinIcon.MAILBOX, "Email", minAccount.getEmail()));

        if (minAccount.getTenant() != null) {
            add(createIconRow(VaadinIcon.BUILDING, "Tenant", minAccount.getTenant()));
        }

        if (minAccount.getFullName() != null && !minAccount.getFullName().equals(minAccount.getEmail())) {
            add(createIconRow(VaadinIcon.USER, "Full name", minAccount.getFullName()));
        }

        if (minAccount.getFunctionRole() != null && !minAccount.getFunctionRole().isBlank()) {
            add(createIconRow(VaadinIcon.COG, "Function role", minAccount.getFunctionRole()));
        }

        String adminFlag = Boolean.TRUE.equals(minAccount.getIsAdmin()) ? "Yes" : "No";
        add(createIconRow(VaadinIcon.SHIELD, "Admin", adminFlag));

        if (minAccount.getAccountType() != null && !minAccount.getAccountType().isBlank()) {
            add(createIconRow(VaadinIcon.TAGS, "Account type", minAccount.getAccountType()));
        }

        if (minAccount.getLastConnectionDate() != null) {
            add(createIconRow(VaadinIcon.CLOCK, "Last login", minAccount.getLastConnectionDate().toString()));
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
        updateStatusChips();
    }

    private void updateStatusChips() {
        if (adminStatusChip != null) {
            String status = minAccount.getAdminStatus() != null ? minAccount.getAdminStatus().name() : "UNKNOWN";
            adminStatusChip.setText(status);
            adminStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            adminStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
        if (systemStatusChip != null) {
            String status = minAccount.getSystemStatus() != null ? minAccount.getSystemStatus().name() : "UNKNOWN";
            systemStatusChip.setText(status);
            systemStatusChip.getElement().setAttribute("title", status);
            ChipColor color = ChipColor.fromStatus(status);
            systemStatusChip.getStyle()
                    .set("background-color", color.background())
                    .set("color", color.foreground());
        }
    }

    public void refresh() {
        getUI().ifPresent(ui -> ui.access(() -> {
            try {
                var response = objectService.findById(minAccount.getId());
                if (response.getBody() != null) {
                    parentView.loadPageZero();
                }
            } catch (Exception e) {
                log.error("Failed to refresh account card", e);
            }
        }));
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .account-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                }
                .account-card .meta-row:last-child {
                    border-bottom: none;
                }
                @media (max-width: 640px) {
                    .account-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .account-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                }
                """;
    }
}