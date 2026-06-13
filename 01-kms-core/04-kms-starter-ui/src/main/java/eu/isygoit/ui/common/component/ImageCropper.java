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

// Cropper.js must be loaded before any executeJs call that uses `new Cropper(...)`.
// The CSS is injected dynamically in the constructor to avoid a separate @StyleSheet
// annotation (which would need the file on the classpath).
@JavaScript("https://cdnjs.cloudflare.com/ajax/libs/cropperjs/1.6.2/cropper.min.js")
public class ImageCropper extends VerticalLayout
        implements HasValue<ImageCropper.ImageValueChangeEvent, MultipartFile> {

    // -------------------------------------------------------------------------
    // Concrete event type — fixes "ValueChangeEvent is abstract; cannot be instantiated"
    // -------------------------------------------------------------------------

    /**
     * Concrete value-change event for {@link ImageCropper}.
     * <p>
     * {@link HasValue.ValueChangeEvent} is abstract, so it can never be instantiated
     * directly. The standard Vaadin pattern for custom composites that are NOT
     * extending {@link AbstractField} is to declare your own concrete
     * {@link ComponentEvent} subclass and bind the {@link HasValue} type parameter
     * to it, which is what this class does.
     */
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
            this.oldValue  = oldValue;
            this.value     = value;
            this.fromClient = fromClient;
        }

        @Override public HasValue<ImageValueChangeEvent, MultipartFile> getHasValue() { return getSource(); }
        @Override public boolean isFromClient() { return fromClient; }
        @Override public MultipartFile getOldValue() { return oldValue; }
        @Override public MultipartFile getValue()    { return value; }
    }

    // -------------------------------------------------------------------------
    // Component state
    // -------------------------------------------------------------------------

    private final Image       previewImage = new Image();
    private final Div         cropperContainer = new Div();
    private final Upload      upload;
    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Button      cropButton;

    private MultipartFile currentCroppedFile;
    private boolean       readOnly                 = false;
    private boolean       requiredIndicatorVisible = false;

    private final List<ValueChangeListener<? super ImageValueChangeEvent>> listeners = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ImageCropper() {
        setWidthFull();
        setSpacing(true);
        addClassName("image-cropper");

        // Inject Cropper.js CSS at runtime — keeps the annotation list minimal
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
        upload.setMaxFileSize(5 * 1024 * 1024); // 5 MB
        upload.addSucceededListener(event -> loadImage(event.getMIMEType(), buffer.getInputStream()));
        add(upload);

        // Assign a stable DOM id so JavaScript can locate the element reliably.
        // this.$.x is Polymer/shadow-DOM syntax and does NOT work in Vaadin Flow's
        // plain server-side component model.
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

    // -------------------------------------------------------------------------
    // Image handling
    // -------------------------------------------------------------------------

    private void loadImage(String mimeType, InputStream inputStream) {
        try {
            byte[] bytes  = inputStream.readAllBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + mimeType + ";base64," + base64;
            previewImage.setSrc(dataUrl);
            previewImage.setVisible(true);
            cropButton.setVisible(true);
            initCropper();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initCropper() {
        // Destroy any previous Cropper instance before re-initialising (e.g. on re-upload).
        // The instance is stored on the element as _cropper to avoid colliding with any
        // native DOM property named "cropper".
        getElement().executeJs(
                "const img = document.getElementById('cropper-img');" +
                        "if (img._cropper) { img._cropper.destroy(); img._cropper = null; }" +
                        "img._cropper = new Cropper(img, { aspectRatio: 1, viewMode: 1, autoCropArea: 0.8 });"
        );
    }

    private void cropImage() {
        // $0 is the server-side element passed as the first extra argument to executeJs,
        // which gives us a reliable reference to call $server on.
        // The result of executeJs itself is NOT used here — the actual crop data arrives
        // asynchronously through the @ClientCallable method below.
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
            String[] parts    = dataUrl.split(",");
            String mimeType   = parts[0].split(":")[1].split(";")[0];
            byte[] imageBytes = java.util.Base64.getDecoder().decode(parts[1]);

            MultipartFile oldValue = this.currentCroppedFile;
            this.currentCroppedFile = new Base64MultipartFile(
                    imageBytes,
                    "cropped_tenant_image." + getExtension(mimeType),
                    mimeType
            );

            previewImage.setSrc(dataUrl);
            cropButton.setVisible(false);

            // Fire to all registered HasValue listeners using our concrete event type
            ImageValueChangeEvent event =
                    new ImageValueChangeEvent(this, oldValue, this.currentCroppedFile, true);
            listeners.forEach(l -> l.valueChanged(event));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getExtension(String mimeType) {
        return switch (mimeType) {
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg"; // covers image/jpeg and image/jpg
        };
    }

    // -------------------------------------------------------------------------
    // HasValue implementation
    // -------------------------------------------------------------------------

    @Override
    public Registration addValueChangeListener(
            ValueChangeListener<? super ImageValueChangeEvent> listener) {
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

    // -------------------------------------------------------------------------
    // Base64MultipartFile — lightweight MultipartFile backed by a byte array
    // -------------------------------------------------------------------------

    private static class Base64MultipartFile implements MultipartFile {

        private final byte[] content;
        private final String name;
        private final String contentType;

        public Base64MultipartFile(byte[] content, String name, String contentType) {
            this.content     = content;
            this.name        = name;
            this.contentType = contentType;
        }

        @Override public String  getName()             { return name; }
        @Override public String  getOriginalFilename() { return name; }
        @Override public String  getContentType()      { return contentType; }
        @Override public boolean isEmpty()             { return content.length == 0; }
        @Override public long    getSize()             { return content.length; }
        @Override public byte[]  getBytes()            { return content; }
        @Override public InputStream getInputStream()  { return new ByteArrayInputStream(content); }
        @Override public void transferTo(java.io.File dest) throws IOException { }
    }
}