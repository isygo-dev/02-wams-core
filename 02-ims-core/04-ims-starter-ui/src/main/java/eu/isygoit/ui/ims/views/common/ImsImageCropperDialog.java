package eu.isygoit.ui.ims.views.common;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

public class ImsImageCropperDialog extends ImageCropperDialog {

    private TabSheet tabs;

    public ImsImageCropperDialog(Consumer<MultipartFile> onImageCropped) {
        super(onImageCropped);
    }

    @Override
    public void add(Component... components) {
        for (Component component : components) {
            if (component instanceof TabSheet) {
                super.add(component);
                continue;
            }
            if (tabs == null) {
                tabs = new TabSheet();
                tabs.addClassName("wams-dialog-tabs");
                tabs.addClassName("ims-dialog-tabs");
                super.add(tabs);
            }
            VerticalLayout page = new VerticalLayout(component);
            page.setPadding(false);
            page.setSpacing(false);
            page.setWidthFull();
            page.addClassName("wams-dialog-tab-content");
            tabs.add(I18n.t("ims.dialog.tab.general"), page);
        }
    }
}
