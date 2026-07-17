package eu.isygoit.ui.ims.views.registered;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.request.RegisteredUserDto;
import eu.isygoit.enums.IEnumRegistrationStatus;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.remote.ims.RegisteredUserService;
import eu.isygoit.remote.ims.TenantService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.registered.dialog.CreateAccountConfirmationDialog;
import eu.isygoit.ui.ims.views.registered.dialog.DeleteRegisteredUserDialog;
import eu.isygoit.ui.ims.views.registered.dialog.RegisteredUserDetailsViewDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RegisteredCard extends BaseCard<RegisteredManagementView, RegisteredUserService> {

    private final RegisteredUserDto registeredUser;
    private final AccountService accountService;
    private final TenantService tenantService;
    private final Runnable onRefresh;

    public RegisteredCard(RegisteredManagementView parentView,
                          RegisteredUserService registeredUserService,
                          AccountService accountService,
                          TenantService tenantService,
                          RegisteredUserDto registeredUser,
                          Runnable onRefresh) {
        super(parentView, registeredUserService);
        this.registeredUser = registeredUser;
        this.accountService = accountService;
        this.tenantService = tenantService;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "registered-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("card-title-group");

        String fullName = (registeredUser.getFirstName() != null ? registeredUser.getFirstName() : "") +
                " " + (registeredUser.getLastName() != null ? registeredUser.getLastName() : "");
        Span titleSpan = buildTitleSpan(fullName.trim(), registeredUser.getEmail());

        // Origin chip
        Span originChip = buildStatusChip(
                registeredUser.getOrigin() != null
                        ? registeredUser.getOrigin().name()
                        : I18n.t("ims.registered.card.status.unknown"),
                registeredUser.getOrigin() != null
                        ? registeredUser.getOrigin().name()
                        : I18n.t("ims.registered.card.status.unknown")
        );

        // Status chip
        Span statusChip = buildStatusChip();

        titleLayout.add(titleSpan, originChip, statusChip);
        return titleLayout;
    }

    private Span buildStatusChip() {
        IEnumRegistrationStatus.Types status = registeredUser.getStatus();

        Span chip = new Span();
        chip.addClassName("wams-chip");

        if (status == null) {
            chip.setText(I18n.t("ims.registered.card.status.unknown"));
            chip.addClassName("wams-chip--neutral");
            return chip;
        }

        switch (status) {
            case PROCESSED:
                chip.setText(I18n.t("ims.registered.card.status.processed"));
                chip.addClassName("wams-chip--success");
                chip.getElement().setAttribute("title", I18n.t("ims.registered.card.status.processed.tooltip"));
                break;
            case CONFIRMED:
                chip.setText(I18n.t("ims.registered.card.status.confirmed"));
                chip.addClassName("wams-chip--primary");
                chip.getElement().setAttribute("title", I18n.t("ims.registered.card.status.confirmed.tooltip"));
                break;
            case REJECTED:
                chip.setText(I18n.t("ims.registered.card.status.rejected"));
                chip.addClassName("wams-chip--error");
                chip.getElement().setAttribute("title", I18n.t("ims.registered.card.status.rejected.tooltip"));
                break;
            case NEW:
            default:
                chip.setText(I18n.t("ims.registered.card.status.new"));
                chip.addClassName("wams-chip--warning");
                chip.getElement().setAttribute("title", I18n.t("ims.registered.card.status.new.tooltip"));
                break;
        }

        return chip;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.registered.card.details.tooltip"),
                () -> new RegisteredUserDetailsViewDialog(parentView, objectService, registeredUser.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.registered.card.edit.tooltip"),
                () -> parentView.openUpdateRegisteredUserDialog(registeredUser, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button createAccountBtn = createCreateAccountButton();

        Button deleteBtn = createDeleteButton(I18n.t("ims.registered.card.delete.tooltip"),
                () -> new DeleteRegisteredUserDialog(parentView, objectService, registeredUser.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, createAccountBtn, deleteBtn);
    }

    private Button createCreateAccountButton() {
        Button btn = new Button();
        btn.setIcon(VaadinIcon.USER_CHECK.create());
        btn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_TERTIARY);
        btn.addClassName("card-action-btn");

        IEnumRegistrationStatus.Types status = registeredUser.getStatus();

        // Enable only if status is CONFIRMED or NEW
        boolean isEnabled = status == IEnumRegistrationStatus.Types.CONFIRMED ||
                status == IEnumRegistrationStatus.Types.NEW;

        btn.setEnabled(isEnabled);

        if (status == IEnumRegistrationStatus.Types.PROCESSED) {
            btn.setTooltipText(I18n.t("ims.registered.card.create.account.disabled.processed.tooltip"));
        } else if (status == IEnumRegistrationStatus.Types.REJECTED) {
            btn.setTooltipText(I18n.t("ims.registered.card.create.account.disabled.rejected.tooltip"));
        } else if (isEnabled) {
            btn.setTooltipText(I18n.t("ims.registered.card.create.account.tooltip"));
        }

        btn.addClickListener(e -> {
            if (status == IEnumRegistrationStatus.Types.NEW) {
                // Show PIN confirmation dialog for NEW status
                showCreateAccountConfirmationDialog();
            } else if (status == IEnumRegistrationStatus.Types.CONFIRMED) {
                // Proceed directly for CONFIRMED status
                proceedWithAccountCreation();
            }
        });

        return btn;
    }

    private void showCreateAccountConfirmationDialog() {
        new CreateAccountConfirmationDialog(
                parentView,
                registeredUser.getEmail(),
                this::proceedWithAccountCreation,
                () -> {
                    if (onRefresh != null) onRefresh.run();
                }
        ).open();
    }

    private void proceedWithAccountCreation() {
        parentView.openCreateAccountFromRegisteredDialog(
                registeredUser,
                () -> {
                    if (onRefresh != null) onRefresh.run();
                }
        );
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("card-row--spaced");

        body.add(createIconRow(VaadinIcon.ENVELOPE, I18n.t("ims.registered.card.email"), registeredUser.getEmail()));
        body.add(createIconRow(VaadinIcon.PHONE, I18n.t("ims.registered.card.phone"), registeredUser.getPhoneNumber()));
        body.add(createIconRow(VaadinIcon.BUILDING, I18n.t("ims.registered.card.organisation"), registeredUser.getOrganisation()));

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

        Span valueSpan = new Span(value != null ? value : I18n.t("ims.registered.card.value.empty"));
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.addClassName("meta-row-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }
}