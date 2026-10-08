package eu.isygoit.ui.sms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.DetailsViewDialog;

public abstract class SmsDetailsDialog extends DetailsViewDialog {

    private TabSheet tabs;
    private VerticalLayout fallbackPage;

    protected SmsDetailsDialog(String title) {
        super(title);
    }

    protected final void addTab(String label, Component content) {
        VerticalLayout page = new VerticalLayout(content);
        page.setPadding(false);
        page.setSpacing(false);
        page.setWidthFull();
        page.addClassName("wams-dialog-tab-content");
        getOrCreateTabs().add(label, page);
    }

    @Override
    public void add(Component... components) {
        if (fallbackPage == null) {
            fallbackPage = new VerticalLayout();
            fallbackPage.setPadding(false);
            fallbackPage.setSpacing(false);
            fallbackPage.setWidthFull();
            getOrCreateTabs().add(I18n.t("sms.dialog.tab.details"), fallbackPage);
        }
        fallbackPage.add(components);
    }

    private TabSheet getOrCreateTabs() {
        if (tabs == null) {
            tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("sms-dialog-tabs");
            super.add(tabs);
        }
        return tabs;
    }
}
