package eu.isygoit.ui.cms.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.PinBaseActionDialog;

public abstract class CmsPinActionDialog extends PinBaseActionDialog {

    private VerticalLayout confirmationPage;

    protected CmsPinActionDialog(String title, String warningMessage, Runnable onSuccess, boolean requirePin) {
        super(title, warningMessage, onSuccess, requirePin);
    }

    protected CmsPinActionDialog(String title, String warningMessage, Runnable onSuccess) {
        super(title, warningMessage, onSuccess);
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            if (containsTabs(component)) {
                super.add(component);
            } else {
                getConfirmationPage().add(component);
            }
        }
    }

    private VerticalLayout getConfirmationPage() {
        if (confirmationPage == null) {
            confirmationPage = new VerticalLayout();
            confirmationPage.setPadding(false);
            confirmationPage.setSpacing(true);
            confirmationPage.setWidthFull();
            TabSheet tabs = new TabSheet();
            tabs.addClassName("wams-dialog-tabs");
            tabs.addClassName("cms-dialog-tabs");
            tabs.add(I18n.t("cms.calendar.dialog.tab.confirmation"), confirmationPage);
            super.add(tabs);
        }
        return confirmationPage;
    }

    private boolean containsTabs(Component component) {
        return component instanceof TabSheet
                || component instanceof Tabs
                || component.getChildren().anyMatch(this::containsTabs);
    }
}
