package eu.isygoit.ui.ims.views.common;

import eu.isygoit.ui.common.dialog.ImageCropperDialog;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

public class ImsImageCropperDialog extends ImageCropperDialog {

    public ImsImageCropperDialog(Consumer<MultipartFile> onImageCropped) {
        super(onImageCropped);
        addClassName("ims-dialog");
    }
}
