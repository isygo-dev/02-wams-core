package eu.isygoit.ui.ims.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class ImsPinActionDialog extends PinBaseActionDialog {

    private TabSheet tabs;
    private VerticalLayout generalPage;

    protected ImsPinActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }

    protected ImsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
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
            tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("ims-dialog-tabs");
            tabs.add(I18n.t("ims.dialog.tab.general"), generalPage);
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
