package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.component.ImageCropper;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

/**
 * Has a real commit action (crop + apply), so it extends {@link BaseActionDialog}
 * rather than {@link NoActionDialog}.
 */
public class ImageCropperDialog extends BaseActionDialog {

    private final ImageCropper imageCropper;
    private final Consumer<MultipartFile> onImageCropped;

    public ImageCropperDialog(Consumer<MultipartFile> onImageCropped) {
        super(I18n.t("common.dialog.image.cropper.title"));
        this.onImageCropped = onImageCropped;

        setWidth("90%");
        setMaxWidth("500px");
        setDraggable(false);
        setResizable(false);

        setOkButtonText(I18n.t("common.dialog.image.cropper.apply"));
        setCancelButtonText(I18n.t("common.dialog.image.cropper.cancel"));
        addThemeVariantsOkButton(ButtonVariant.LUMO_PRIMARY);
        enableOkButton(false);

        imageCropper = new ImageCropper();
        imageCropper.setWidthFull();
        imageCropper.addValueChangeListener(event -> enableOkButton(event.getValue() != null));

        Div content = new Div(imageCropper);
        content.addClassName("wams-image-cropper-dialog__content");
        addContent(content);
    }

    @Override
    protected boolean onOk() {
        MultipartFile cropped = imageCropper.getValue();
        if (cropped == null) {
            append(I18n.t("common.dialog.image.cropper.no.image"));
            return false;
        }
        if (onImageCropped != null) {
            onImageCropped.accept(cropped);
        }
        append(I18n.t("common.dialog.image.cropper.crop.success"));
        return true;
    }
}
