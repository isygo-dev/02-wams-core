package eu.isygoit.ui.ims.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

public abstract class ImsDetailsDialog extends DetailsViewDialog {

    private TabSheet tabs;

    protected ImsDetailsDialog(String title) {
        super(title);
        addClassName("ims-dialog");
    }

    protected final void addTab(String label, Component content) {
        TabSheet tabSheet = getOrCreateTabs();
        VerticalLayout page = new VerticalLayout(content);
        page.setPadding(false);
        page.setSpacing(false);
        page.setWidthFull();
        page.addClassName("wams-dialog-tab-content");
        tabSheet.add(label, page);
    }

    private TabSheet getOrCreateTabs() {
        if (tabs == null) {
            tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("ims-dialog-tabs");
            super.add(tabs);
        }
        return tabs;
    }
}
