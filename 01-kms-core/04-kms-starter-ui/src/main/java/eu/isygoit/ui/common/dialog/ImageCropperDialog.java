package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ImageCropper;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

public class ImageCropperDialog extends Dialog {

    private final ImageCropper imageCropper;
    private final Button applyButton;

    public ImageCropperDialog(Consumer<MultipartFile> onImageCropped) {
        setHeaderTitle(I18n.t("common.dialog.image.cropper.title"));
        setWidth("90%");
        setMaxWidth("500px");
        setDraggable(false);
        setResizable(false);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        imageCropper = new ImageCropper();
        imageCropper.setWidthFull();

        applyButton = new Button(I18n.t("common.dialog.image.cropper.apply"), e -> {
            MultipartFile cropped = imageCropper.getValue();
            if (cropped != null && onImageCropped != null) {
                onImageCropped.accept(cropped);
                Notification.show(I18n.t("common.dialog.image.cropper.crop.success"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show(I18n.t("common.dialog.image.cropper.no.image"), 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING);
            }
            close();
        });
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.setEnabled(false);

        imageCropper.addValueChangeListener(event -> {
            applyButton.setEnabled(event.getValue() != null);
        });

        Button cancelButton = new Button(I18n.t("common.dialog.image.cropper.cancel"), e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, applyButton);
        buttonLayout.setSpacing(true);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();

        VerticalLayout layout = new VerticalLayout(imageCropper, buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setWidthFull();

        Div content = new Div(layout);
        content.getStyle().set("overflow", "auto");
        add(content);
    }
}