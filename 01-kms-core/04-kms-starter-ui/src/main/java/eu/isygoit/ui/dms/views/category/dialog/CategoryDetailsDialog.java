package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.dto.data.CategoryDto;
import eu.isygoit.helper.DateHelper;
import eu.isygoit.i18n.I18n;
import eu.isygoit.remote.dms.CategoryService;
import eu.isygoit.ui.common.dialog.NoActionDialog;
import eu.isygoit.ui.dms.views.category.CategoryManagementView;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

public class CategoryDetailsDialog extends NoActionDialog {

    private final CategoryManagementView parentView;
    private final CategoryService categoryService;
    private final Long categoryId;

    public CategoryDetailsDialog(CategoryManagementView parentView,
                                 CategoryService categoryService,
                                 Long categoryId) {
        super(I18n.t("dms.category.details.title"));
        this.parentView = parentView;
        this.categoryService = categoryService;
        this.categoryId = categoryId;

        setWidth("700px");
        setMaxWidth("95%");
        setModal(true);
        setDraggable(true);
        setResizable(true);
        addClassName("category-details-dialog");

        loadAndShowDetails();
    }

    private void loadAndShowDetails() {
        parentView.showLoading(true);
        try {
            ResponseEntity<CategoryDto> response = categoryService.findById(categoryId);
            if (response.getBody() != null) {
                buildContent(response.getBody());
            } else {
                add(new Span(I18n.t("dms.category.details.not.found")));
                addCloseButton();
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("dms.category.details.load.error", extractErrorMessage(ex))));
            addCloseButton();
        } catch (Exception e) {
            add(new Span(I18n.t("dms.category.details.load.error", e.getMessage())));
            addCloseButton();
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CategoryDto category) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        Div infoGrid = new Div();
        infoGrid.addClassName("details-grid");

        addFieldToGrid(infoGrid, VaadinIcon.HASH, I18n.t("dms.category.details.field.id"), category.getId() != null ? String.valueOf(category.getId()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.TAG, I18n.t("dms.category.details.field.name"), category.getName());
        addFieldToGrid(infoGrid, VaadinIcon.USER_CHECK, I18n.t("dms.category.details.field.created.by"), category.getCreatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR, I18n.t("dms.category.details.field.created.date"),
                category.getCreateDate() != null ? DateHelper.formatToHumanReadable(category.getCreateDate()) : null);
        addFieldToGrid(infoGrid, VaadinIcon.EDIT, I18n.t("dms.category.details.field.updated.by"), category.getUpdatedBy());
        addFieldToGrid(infoGrid, VaadinIcon.CALENDAR_O, I18n.t("dms.category.details.field.updated.date"),
                category.getUpdateDate() != null ? DateHelper.formatToHumanReadable(category.getUpdateDate()) : null);

        mainLayout.add(createSection(I18n.t("dms.category.details.section.info"), infoGrid));

        // Description is rendered as its own full-width block so long text is
        // never clipped/truncated by the fixed-width info grid columns.
        if (category.getDescription() != null && !category.getDescription().isBlank()) {
            mainLayout.add(createSection(I18n.t("dms.category.details.section.description"),
                    createDescriptionBlock(category.getDescription())));
        }

        add(mainLayout);
        addCloseButton();
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName("detail-field");

        Icon iconComponent = icon.create();
        iconComponent.setSize("16px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label + ":");
        labelSpan.addClassName(LumoUtility.FontWeight.SEMIBOLD);
        labelSpan.addClassName(LumoUtility.FontSize.SMALL);

        Span valueSpan = new Span(value);
        valueSpan.addClassName(LumoUtility.FontSize.SMALL);
        valueSpan.addClassName("detail-field-value");

        row.add(iconComponent, labelSpan, valueSpan);
        row.expand(valueSpan);
        container.add(row);
    }

    private Component createDescriptionBlock(String description) {
        Span descriptionSpan = new Span(description);
        descriptionSpan.addClassName(LumoUtility.FontSize.SMALL);
        descriptionSpan.addClassName("detail-field-value");
        // Guarantee the full text is always visible: wrap on words/lines and
        // never clip with an ellipsis, regardless of how long the description is.
        descriptionSpan.getStyle()
                .set("display", "block")
                .set("white-space", "pre-wrap")
                .set("word-break", "break-word")
                .set("overflow", "visible")
                .set("text-overflow", "unset");
        return descriptionSpan;
    }

    private Component createSection(String title, Component content) {
        VerticalLayout section = new VerticalLayout();
        section.setPadding(false);
        section.setSpacing(false);
        Span titleSpan = new Span(title);
        titleSpan.addClassName(LumoUtility.FontWeight.BOLD);
        titleSpan.addClassName(LumoUtility.FontSize.MEDIUM);
        titleSpan.addClassName("wams-section-title");
        section.add(titleSpan, content);
        return section;
    }

    private void addCloseButton() {
        Button closeButton = new Button(I18n.t("dms.category.details.close"), e -> close());
        closeButton.addClassName(LumoUtility.Margin.Top.MEDIUM);
        HorizontalLayout buttonBar = new HorizontalLayout(closeButton);
        buttonBar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonBar.setWidthFull();
        add(buttonBar);
    }

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}