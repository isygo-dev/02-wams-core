package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import eu.isygoit.i18n.I18n;

/**
 * Reusable tabbed base for read-only details dialogs.
 *
 * <p>Callers supply the localized tab labels and may optionally supply a CSS
 * class for the tab sheet. Existing tabbed content is retained as-is; content
 * without a tab navigation is placed in a single tab.</p>
 */
public abstract class TabbedDetailsViewDialog extends DetailsViewDialog {

    private final String detailsTabLabel;
    private final String sectionTabLabel;

    /** Uses the shared localized labels for tab content added without an explicit label. */
    protected TabbedDetailsViewDialog(String title) {
        this(title, I18n.t("common.dialog.tab.details"), I18n.t("common.dialog.tab.section"));
    }

    protected TabbedDetailsViewDialog(String title, String detailsTabLabel, String sectionTabLabel) {
        super(title);
        this.detailsTabLabel = detailsTabLabel;
        this.sectionTabLabel = sectionTabLabel;
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            if (containsTabs(component)) {
                super.add(component);
                continue;
            }
            TabSheet tabs = findTabSheet();
            long existingTabs = tabs == null ? 0 : tabCount(tabs);
            String label = existingTabs == 0
                    ? detailsTabLabel
                    : sectionTabLabel + " " + (existingTabs + 1);
            addTab(label, component);
        }
    }

    /**
     * Adds one logical read-only section as its own localized tab.
     */
    protected final void addTab(String label, Component content) {
        TabSheet tabs = findTabSheet();
        if (tabs == null) {
            tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            super.add(tabs);
        }

        long existingTabs = tabCount(tabs);
        String tabLabel = label;
        if (tabLabel == null || tabLabel.isBlank()) {
            tabLabel = existingTabs == 0
                    ? detailsTabLabel
                    : sectionTabLabel + " " + (existingTabs + 1);
        }

        VerticalLayout page = new VerticalLayout(content);
        page.setPadding(false);
        page.setSpacing(false);
        page.setWidthFull();
        page.addClassName("wams-dialog-tab-content");
        tabs.add(tabLabel, page);
    }

    /** Applies the shared width scale (see {@link DialogLayout}) to the dialog. */
    protected final void applyWidth(String width) {
        DialogLayout.size(this, width);
    }

    /** Value shown for an empty text field. */
    protected static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** Value shown for an empty object field. */
    protected static String dash(Object value) {
        return value == null ? "-" : value.toString();
    }

    /** Audit tab: who created / updated the record, and when. */
    protected final void addAuditTab(String createdBy, Object createDate, String updatedBy, Object updateDate) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("common.audit.createdBy"), dash(createdBy));
        addFieldToGrid(grid, VaadinIcon.CALENDAR, I18n.t("common.audit.createdAt"), dash(createDate));
        addFieldToGrid(grid, VaadinIcon.USER, I18n.t("common.audit.updatedBy"), dash(updatedBy));
        addFieldToGrid(grid, VaadinIcon.CALENDAR, I18n.t("common.audit.updatedAt"), dash(updateDate));
        addTab(I18n.t("common.details.section.audit"),
                createSection(I18n.t("common.details.section.audit"), grid));
    }

    /** File metadata tab for entities carrying a primary file (FileEntityDto). */
    protected final void addFileMetadataTab(String fileName, String originalFileName, String path,
                                            String extension, String type, String category) {
        Div grid = createDetailGrid();
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("common.file.name"), dash(fileName));
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT_O, I18n.t("common.file.originalName"), dash(originalFileName));
        addFieldToGrid(grid, VaadinIcon.FOLDER, I18n.t("common.file.path"), dash(path));
        addFieldToGrid(grid, VaadinIcon.FILE_TEXT, I18n.t("common.file.extension"), dash(extension));
        addFieldToGrid(grid, VaadinIcon.TAG, I18n.t("common.file.type"), dash(type));
        addFieldToGrid(grid, VaadinIcon.TAG, I18n.t("common.file.category"), dash(category));
        addTab(I18n.t("common.details.section.fileMetadata"),
                createSection(I18n.t("common.details.section.fileMetadata"), grid));
    }

    private TabSheet findTabSheet() {
        return getChildren()
                .filter(TabSheet.class::isInstance)
                .map(TabSheet.class::cast)
                .findFirst()
                .orElse(null);
    }

    private long tabCount(TabSheet tabs) {
        return tabs.getChildren()
                .filter(Tabs.class::isInstance)
                .map(Tabs.class::cast)
                .mapToLong(childTabs -> childTabs.getChildren().count())
                .findFirst()
                .orElse(0);
    }

    private boolean containsTabs(Component component) {
        return component instanceof TabSheet
                || component instanceof Tabs
                || component.getChildren().anyMatch(this::containsTabs);
    }
}
