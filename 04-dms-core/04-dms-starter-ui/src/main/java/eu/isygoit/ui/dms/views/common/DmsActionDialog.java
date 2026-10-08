package eu.isygoit.ui.dms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.BaseActionDialog;

public abstract class DmsActionDialog extends BaseActionDialog {

    private VerticalLayout generalPage;

    protected DmsActionDialog(String title) {
        super(title);
    }

    protected DmsActionDialog(String title, Runnable onSuccess) {
        super(title, onSuccess);
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            if (containsTabs(component)) {
                super.add(component);
            } else {
                getGeneralPage().add(component);
            }
        }
    }

    private VerticalLayout getGeneralPage() {
        if (generalPage == null) {
            generalPage = new VerticalLayout();
            generalPage.setPadding(false);
            generalPage.setSpacing(true);
            generalPage.setWidthFull();
            TabSheet tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("dms-dialog-tabs");
            tabs.add(I18n.t("dms.dialog.tab.general"), generalPage);
            super.add(tabs);
        }
        return generalPage;
    }

    private boolean containsTabs(Component component) {
        return component instanceof TabSheet
                || component instanceof Tabs
                || component.getChildren().anyMatch(this::containsTabs);
    }
}
