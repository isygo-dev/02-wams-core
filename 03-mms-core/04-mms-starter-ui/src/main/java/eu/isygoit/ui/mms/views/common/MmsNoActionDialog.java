package eu.isygoit.ui.mms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

public abstract class MmsNoActionDialog extends NoActionDialog {

    private VerticalLayout contentPage;

    protected MmsNoActionDialog(String title) {
        super(title);
    }

    @Override
    public void add(Component... components) {
        if (contentPage == null) {
            contentPage = new VerticalLayout();
            contentPage.setPadding(false);
            contentPage.setSpacing(true);
            contentPage.setWidthFull();
            TabSheet tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("mms-dialog-tabs");
            tabs.add(I18n.t("mms.dialog.tab.connection"), contentPage);
            super.add(tabs);
        }
        contentPage.add(components);
    }
}
