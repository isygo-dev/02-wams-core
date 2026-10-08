package eu.isygoit.ui.dms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

public abstract class DmsDetailsDialog extends DetailsViewDialog {

    private TabSheet tabs;

    protected DmsDetailsDialog(String title) {
        super(title);
        addClassName("dms-dialog");
    }

    protected final void addTab(String label, Component content) {
        VerticalLayout page = new VerticalLayout(content);
        page.setPadding(false);
        page.setSpacing(false);
        page.setWidthFull();
        page.addClassName("wams-dialog-tab-content");
        getOrCreateTabs().add(label, page);
    }

    private TabSheet getOrCreateTabs() {
        if (tabs == null) {
            tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("dms-dialog-tabs");
            super.add(tabs);
        }
        return tabs;
    }
}
