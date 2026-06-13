package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import eu.isygoit.ui.common.component.ImageCropper;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

public class ImageCropperDialog extends Dialog {

    private final ImageCropper imageCropper;
    private final Consumer<MultipartFile> onImageCropped;

    public ImageCropperDialog(Consumer<MultipartFile> onImageCropped) {
        this.onImageCropped = onImageCropped;

        setHeaderTitle("Crop Image");
        setWidth("90%");
        setMaxWidth("500px");
        setDraggable(false);
        setResizable(false);

        imageCropper = new ImageCropper();
        imageCropper.setWidthFull();

        Button cancelButton = new Button("Cancel", e -> close());
        Button applyButton = new Button("Apply", e -> {
            MultipartFile cropped = imageCropper.getValue();
            if (cropped != null && onImageCropped != null) {
                onImageCropped.accept(cropped);
            }
            close();
        });
        applyButton.addClassName("primary");

        HorizontalLayout buttons = new HorizontalLayout(cancelButton, applyButton);
        buttons.setSpacing(true);

        VerticalLayout layout = new VerticalLayout(imageCropper, buttons);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        Div content = new Div(layout);
        content.getStyle().set("overflow", "auto");
        add(content);
    }
}