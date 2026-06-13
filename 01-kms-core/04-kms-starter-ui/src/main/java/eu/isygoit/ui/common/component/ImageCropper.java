package eu.isygoit.ui.common.component;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.shared.Registration;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@JavaScript("https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.6.2/cropper.min.js")
public class ImageCropper extends VerticalLayout
        implements HasValue<ImageCropper.ImageValueChangeEvent, MultipartFile> {

    public static class ImageValueChangeEvent
            extends ComponentEvent<ImageCropper>
            implements HasValue.ValueChangeEvent<MultipartFile> {

        private final MultipartFile oldValue;
        private final MultipartFile value;
        private final boolean fromClient;

        public ImageValueChangeEvent(ImageCropper source,
                                     MultipartFile oldValue,
                                     MultipartFile value,
                                     boolean fromClient) {
            super(source, fromClient);
            this.oldValue = oldValue;
            this.value = value;
            this.fromClient = fromClient;
        }

        @Override public HasValue<ImageValueChangeEvent, MultipartFile> getHasValue() { return getSource(); }
        @Override public boolean isFromClient() { return fromClient; }
        @Override public MultipartFile getOldValue() { return oldValue; }
        @Override public MultipartFile getValue()    { return value; }
    }

    private final Image previewImage = new Image();
    private final Div cropperContainer = new Div();
    private final Upload upload;
    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Button cropButton;

    private MultipartFile currentCroppedFile;
    private boolean readOnly = false;
    private boolean requiredIndicatorVisible = false;

    private final List<ValueChangeListener<? super ImageValueChangeEvent>> listeners = new ArrayList<>();

    public ImageCropper() {
        setWidthFull();
        setSpacing(true);
        addClassName("image-cropper");

        getElement().executeJs(
                "if (!document.getElementById('cropperjs-css')) {" +
                        "  const l = document.createElement('link');" +
                        "  l.id   = 'cropperjs-css';" +
                        "  l.rel  = 'stylesheet';" +
                        "  l.href = 'https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.6.2/cropper.min.css';" +
                        "  document.head.appendChild(l);" +
                        "}"
        );

        upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/jpg", "image/webp");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.addSucceededListener(event -> loadImage(event.getMIMEType(), buffer.getInputStream()));
        add(upload);

        previewImage.setId("cropper-img");
        previewImage.setWidth("100%");
        previewImage.setHeight("auto");
        previewImage.setVisible(false);

        cropperContainer.add(previewImage);
        cropperContainer.setWidthFull();
        cropperContainer.getStyle().set("text-align", "center");
        add(cropperContainer);

        cropButton = new Button("Crop", e -> cropImage());
        cropButton.setVisible(false);
        add(cropButton);
    }

    private void loadImage(String mimeType, InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64;
            previewImage.setSrc(dataUrl);
            previewImage.setVisible(true);
            cropButton.setVisible(true);

            // Reset cropped value when a new image is uploaded
            MultipartFile oldValue = this.currentCroppedFile;
            this.currentCroppedFile = null;
            cropButton.setEnabled(true); // ensure crop button is enabled

            // Fire value change event (old value -> null) so that listeners (e.g., Apply button) can react
            ImageValueChangeEvent event = new ImageValueChangeEvent(this, oldValue, null, true);
            listeners.forEach(l -> l.valueChanged(event));

            initCropper();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCropper() {
        getElement().executeJs(
                "const img = document.getElementById('cropper-img');" +
                        "if (img._cropper) { img._cropper.destroy(); img._cropper = null; }" +
                        "img._cropper = new Cropper(img, { aspectRatio: 1, viewMode: 1, autoCropArea: 0.8 });"
        );
    }

    private void cropImage() {
        getElement().executeJs(
                "const img = document.getElementById('cropper-img');" +
                        "if (img && img._cropper) {" +
                        "  const canvas     = img._cropper.getCroppedCanvas();" +
                        "  const croppedUrl = canvas.toDataURL();" +
                        "  $0.$server.setCroppedImage(croppedUrl);" +
                        "}",
                getElement()
        );
    }

    @ClientCallable
    public void setCroppedImage(String dataUrl) {
        try {
            String[] parts = dataUrl.split(",");
            String mimeType = parts[0].split(":")[1].split(";")[0];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);

            MultipartFile oldValue = this.currentCroppedFile;
            this.currentCroppedFile = new Base64MultipartFile(
                    imageBytes,
                    "cropped_image." + getExtension(mimeType),
                    mimeType
            );

            previewImage.setSrc(dataUrl);
            cropButton.setVisible(false); // hide crop button after cropping

            // Fire value change with the new cropped file
            ImageValueChangeEvent event = new ImageValueChangeEvent(this, oldValue, this.currentCroppedFile, true);
            listeners.forEach(l -> l.valueChanged(event));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getExtension(String mimeType) {
        return switch (mimeType) {
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    @Override
    public Registration addValueChangeListener(ValueChangeListener<? super ImageValueChangeEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public void setValue(MultipartFile value) {
        this.currentCroppedFile = value;
    }

    @Override
    public MultipartFile getValue() {
        return currentCroppedFile;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        upload.setVisible(!readOnly);
        cropButton.setVisible(!readOnly && currentCroppedFile == null);
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setRequiredIndicatorVisible(boolean visible) {
        this.requiredIndicatorVisible = visible;
    }

    @Override
    public boolean isRequiredIndicatorVisible() {
        return requiredIndicatorVisible;
    }

    private static class Base64MultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public Base64MultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException { }
    }
}