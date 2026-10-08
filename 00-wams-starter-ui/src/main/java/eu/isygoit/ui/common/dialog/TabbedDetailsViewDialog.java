package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;

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
