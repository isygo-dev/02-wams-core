package eu.isygoit.ui.ims.views.roleinfo;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.RoleInfoDto;
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
    protected Component buildTitle() {
        return new Div();
    }

    @Override
    protected String cardCssClassName() {
        return "role-card";
    }

    @Override
    protected void buildHeader() {
        HorizontalLayout headerRow = new HorizontalLayout();
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.setSpacing(true);
        headerRow.getStyle().set("flex-wrap", "wrap");

        Span titleSpan = buildTitleSpan(role.getName(), role.getCode());
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);

        Span levelChip = new Span("Level " + role.getLevel());
        levelChip.getStyle()
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("border-radius", "var(--lumo-border-radius-s)")
                .set("padding", "0.2rem 0.5rem")
                .set("font-size", "var(--lumo-font-size-xs)");

        buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(false);
        buttonBar.getStyle().set("flex-wrap", "wrap");
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.addClassName(cardCssClassName() + "__button-bar");

        List<Button> buttons = buildActionButtons();
        buttons.forEach(buttonBar::add);

        headerRow.add(titleSpan, levelChip, buttonBar);
        headerRow.expand(buttonBar);

        add(headerRow);
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, "View role details");
        detailsBtn.addClickListener(e -> new RoleDetailsDialog(parentView, objectService, role.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, "Edit role (permissions & applications)");
        editBtn.addClickListener(e -> parentView.openUpdateRoleDialog(role, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, "Delete role");
        deleteBtn.addClickListener(e -> new DeleteRoleDialog(parentView, objectService, role.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.getStyle().set("margin-top", "var(--lumo-space-s)");

        body.add(createInfoRow(VaadinIcon.CODE, "Code", role.getCode()));
        body.add(createInfoRow(VaadinIcon.USERS, "Users", String.valueOf(role.getNumberOfUsers())));
        body.add(createInfoRow(VaadinIcon.FUNCTION, "Allowed apps", String.valueOf(role.getAllowedTools() != null ? role.getAllowedTools().size() : 0)));
        body.add(createInfoRow(VaadinIcon.LOCK, "Permissions", String.valueOf(role.getRolePermission() != null ? role.getRolePermission().size() : 0)));
        if (role.getDescription() != null && !role.getDescription().isBlank()) {
            body.add(createInfoRow(VaadinIcon.FILE_TEXT, "Description", role.getDescription()));
        }

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
        iconComponent.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        labelSpan.getStyle().set("min-width", "80px");

        Span valueSpan = new Span(value != null ? value : "—");
        valueSpan.addClassName(LumoUtility.FontSize.XXSMALL);
        valueSpan.getStyle().set("flex", "1");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        return row;
    }

    @Override
    protected void onCardAttach(AttachEvent event) {
    }

    @Override
    protected String buildExtraStyles() {
        return """
                .role-card {
                    padding: var(--lumo-space-s) var(--lumo-space-m);
                }
                .role-card .meta-row {
                    border-bottom: 1px solid var(--lumo-contrast-10pct);
                    padding-bottom: var(--lumo-space-xs);
                    margin-bottom: var(--lumo-space-xs);
                }
                .role-card .meta-row:last-child {
                    border-bottom: none;
                    margin-bottom: 0;
                }
                @media (max-width: 640px) {
                    .role-card .meta-row {
                        flex-wrap: wrap;
                    }
                    .role-card .meta-row > :not(:first-child) {
                        margin-left: 28px;
                    }
                    .role-card .role-card__button-bar {
                        width: 100%;
                        justify-content: flex-start;
                    }
                }
                """;
    }
}