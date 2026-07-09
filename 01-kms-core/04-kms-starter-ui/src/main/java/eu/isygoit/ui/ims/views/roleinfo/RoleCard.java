package eu.isygoit.ui.ims.views.roleinfo;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.RoleInfoDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.ims.RoleInfoService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.ims.views.roleinfo.dialog.DeleteRoleDialog;
import eu.isygoit.ui.ims.views.roleinfo.dialog.RoleDetailsDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class RoleCard extends BaseCard<RoleManagementView, RoleInfoService> {

    private final RoleInfoDto role;
    private final Runnable onRefresh;

    public RoleCard(RoleManagementView parentView,
                    RoleInfoService roleService,
                    RoleInfoDto role,
                    Runnable onRefresh) {
        super(parentView, roleService);
        this.role = role;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "role-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("card-title-group");

        Span titleSpan = buildTitleSpan(role.getName(), role.getCode());
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);

        Span levelChip = new Span(I18n.t("ims.role.card.level", role.getLevel()));
        levelChip.addClassName("level-chip");

        titleLayout.add(titleSpan, levelChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createDetailsButton(I18n.t("ims.role.card.details.tooltip"),
                () -> new RoleDetailsDialog(parentView, objectService, role.getId()).open());

        Button editBtn = createEditButton(I18n.t("ims.role.card.edit.tooltip"),
                () -> parentView.openUpdateRoleDialog(role, () -> {
                    if (onRefresh != null) onRefresh.run();
                }));

        Button deleteBtn = createDeleteButton(I18n.t("ims.role.card.delete.tooltip"),
                () -> new DeleteRoleDialog(parentView, objectService, role.getId(), () -> {
                    if (onRefresh != null) onRefresh.run();
                }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("card-row--spaced");

        body.add(createInfoRow(VaadinIcon.CODE, I18n.t("ims.role.card.code"), role.getCode()));
        body.add(createInfoRow(VaadinIcon.USERS, I18n.t("ims.role.card.users"), String.valueOf(role.getNumberOfUsers())));

        add(body);
    }

    private HorizontalLayout createInfoRow(VaadinIcon icon, String label, String value) {
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

    @Override
    protected void onCardAttach(AttachEvent event) {
    }
}