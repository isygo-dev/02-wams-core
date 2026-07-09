package eu.isygoit.ui.dms.views.category.dialog;

import com.vaadin.flow.component.Component;
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
            }
        } catch (FeignException ex) {
            add(new Span(I18n.t("dms.category.details.load.error", extractErrorMessage(ex))));
        } catch (Exception e) {
            add(new Span(I18n.t("dms.category.details.load.error", e.getMessage())));
        } finally {
            parentView.showLoading(false);
        }
    }

    private void buildContent(CategoryDto category) {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(false);
        mainLayout.setSpacing(true);

        // Identity section: the fields that identify/describe the category itself.
        Div identityGrid = new Div();
        identityGrid.addClassName("wams-card__detail-grid");

        addFieldToGrid(identityGrid, VaadinIcon.HASH, I18n.t("dms.category.details.field.id"), category.getId() != null ? String.valueOf(category.getId()) : null);
        addFieldToGrid(identityGrid, VaadinIcon.TAG, I18n.t("dms.category.details.field.name"), category.getName());

        mainLayout.add(createSection(I18n.t("dms.category.details.section.identity"), identityGrid));

        // Description is rendered as its own full-width block so long text is
        // never clipped/truncated by the fixed-width identity grid columns.
        if (category.getDescription() != null && !category.getDescription().isBlank()) {
            mainLayout.add(createSection(I18n.t("dms.category.details.section.description"),
                    createDescriptionBlock(category.getDescription())));
        }

        // Audit section: who created/updated the category and when.
        Div auditGrid = new Div();
        auditGrid.addClassName("wams-card__detail-grid");

        addFieldToGrid(auditGrid, VaadinIcon.USER_CHECK, I18n.t("dms.category.details.field.created.by"), category.getCreatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR, I18n.t("dms.category.details.field.created.date"),
                category.getCreateDate() != null ? DateHelper.formatToHumanReadable(category.getCreateDate()) : null);
        addFieldToGrid(auditGrid, VaadinIcon.EDIT, I18n.t("dms.category.details.field.updated.by"), category.getUpdatedBy());
        addFieldToGrid(auditGrid, VaadinIcon.CALENDAR_O, I18n.t("dms.category.details.field.updated.date"),
                category.getUpdateDate() != null ? DateHelper.formatToHumanReadable(category.getUpdateDate()) : null);

        mainLayout.add(createSection(I18n.t("dms.category.details.section.audit"), auditGrid));

        add(mainLayout);
    }

    private void addFieldToGrid(Div container, VaadinIcon icon, String label, String value) {
        if (value == null || value.isBlank()) return;

        VerticalLayout field = new VerticalLayout();
        field.setPadding(false);
        field.setSpacing(false);
        field.addClassName("wams-card__detail-field");

        HorizontalLayout labelRow = new HorizontalLayout();
        labelRow.setAlignItems(FlexComponent.Alignment.CENTER);
        labelRow.setSpacing(false);
        labelRow.addClassName("wams-card__detail-field-label-row");

        Icon iconComponent = icon.create();
        iconComponent.setSize("12px");
        iconComponent.addClassName("detail-field-icon");

        Span labelSpan = new Span(label);
        labelSpan.addClassName("wams-card__detail-field-label");

        labelRow.add(iconComponent, labelSpan);

        Span valueSpan = new Span(value);
        valueSpan.addClassName("wams-card__detail-field-value");

        field.add(labelRow, valueSpan);
        container.add(field);
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

    private String extractErrorMessage(FeignException ex) {
        try {
            if (ex.contentUTF8() != null && !ex.contentUTF8().isBlank())
                return ex.contentUTF8();
        } catch (Exception ignored) {
        }
        return ex.getMessage();
    }
}