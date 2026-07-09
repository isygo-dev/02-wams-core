package eu.isygoit.ui.dms.views.category;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.card.BaseCard;
import eu.isygoit.ui.dms.views.category.dialog.CategoryDetailsDialog;
import eu.isygoit.ui.dms.views.category.dialog.DeleteCategoryDialog;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CategoryCard extends BaseCard<CategoryManagementView, CategoryService> {

    private final CategoryDto category;
    private final Runnable onRefresh;

    public CategoryCard(CategoryManagementView parentView,
                        CategoryService categoryService,
                        CategoryDto category,
                        Runnable onRefresh) {
        super(parentView, categoryService);
        this.category = category;
        this.onRefresh = onRefresh;
        initCard();
    }

    @Override
    protected String cardCssClassName() {
        return "category-card";
    }

    @Override
    protected Component buildTitle() {
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        titleLayout.setSpacing(true);
        titleLayout.addClassName("wams-title-row");

        Span titleSpan = buildTitleSpan(category.getName(), category.getDescription());

        // Add ID chip
        Span idChip = buildStatusChip(
                I18n.t("dms.category.card.id.chip", category.getId()),
                "info"
        );

        titleLayout.add(titleSpan, idChip);
        return titleLayout;
    }

    @Override
    protected List<Button> buildActionButtons() {
        Button detailsBtn = createIconButton(VaadinIcon.INFO_CIRCLE, I18n.t("dms.category.card.details.tooltip"));
        detailsBtn.addClickListener(e -> new CategoryDetailsDialog(parentView, objectService, category.getId()).open());

        Button editBtn = createIconButton(VaadinIcon.EDIT, I18n.t("dms.category.card.edit.tooltip"));
        editBtn.addClickListener(e -> parentView.openUpdateCategoryDialog(category, () -> {
            if (onRefresh != null) onRefresh.run();
        }));

        Button deleteBtn = createIconButton(VaadinIcon.TRASH, I18n.t("dms.category.card.delete.tooltip"));
        deleteBtn.addClickListener(e -> new DeleteCategoryDialog(parentView, objectService, category.getId(), () -> {
            if (onRefresh != null) onRefresh.run();
        }).open());

        return List.of(detailsBtn, editBtn, deleteBtn);
    }

    @Override
    protected void buildBodyRows() {
        VerticalLayout body = new VerticalLayout();
        body.setSpacing(false);
        body.setPadding(false);
        body.addClassName("wams-body-rows");

        body.add(createIconRow(VaadinIcon.USER, I18n.t("dms.category.card.created.by"), category.getCreatedBy()));
        body.add(createIconRow(VaadinIcon.CALENDAR, I18n.t("dms.category.card.created.date"),
                category.getCreateDate() != null ? category.getCreateDate().toString() : null));

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

    @Override
    protected void onCardAttach(AttachEvent event) {
        // nothing special
    }
}