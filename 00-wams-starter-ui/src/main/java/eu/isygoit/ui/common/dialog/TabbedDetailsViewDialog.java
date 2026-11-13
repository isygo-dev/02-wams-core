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

    /**
     * Uses the shared localized labels for tab content added without an explicit label.
     */
    protected TabbedDetailsViewDialog(String title) {
        this(title, I18n.t("common.dialog.tab.details"), I18n.t("common.dialog.tab.section"));
    }

    protected TabbedDetailsViewDialog(String title, String detailsTabLabel, String sectionTabLabel) {
        super(title);
        this.detailsTabLabel = detailsTabLabel;
        this.sectionTabLabel = sectionTabLabel;
    }

    /**
     * Value shown for an empty text field.
     */
    protected static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /**
     * Value shown for an empty object field.
     */
    protected static String dash(Object value) {
        return value == null ? "-" : value.toString();
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

    /**
     * Applies the shared width scale (see {@link DialogLayout}) to the dialog.
     */
    protected final void applyWidth(String width) {
        DialogLayout.size(this, width);
    }

    /**
     * Audit: who created / updated the record, and when. It is no longer a tab: the values go to the audit
     * cartouche at the bottom of the dialog ({@link #setAudit(String, Object, String, Object)}), so existing
     * callers keep working unchanged.
     */
    protected final void addAuditTab(String createdBy, Object createDate, String updatedBy, Object updateDate) {
        setAudit(createdBy, createDate, updatedBy, updateDate);
    }

    /**
     * The "Files & documents" tab: every file of the object as a list (name, size, type, download), made by the
     * caller from its file managers. It replaces the old file metadata tab (file name, original name, path,
     * extension, type, category), which is no longer shown.
     */
    protected final void addFilesTab(Component... fileLists) {
        com.vaadin.flow.component.orderedlayout.VerticalLayout content =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(fileLists);
        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        addTab(I18n.t("common.details.section.files"),
                createSection(I18n.t("common.details.section.files"), content));
    }

    /**
     * File metadata tab for entities carrying a primary file (FileEntityDto).
     * @deprecated no longer shown: use {@link #addFilesTab(Component...)}; kept so older callers keep compiling.
     */
    @Deprecated
    @SuppressWarnings("unused")
    protected final void addFileMetadataTab(String fileName, String originalFileName, String path,
                                            String extension, String type, String category) {
        // intentionally empty: the file metadata is not displayed any more
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
