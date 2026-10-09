package eu.isygoit.ui.common.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

/**
 * Profile photo "hero" shown on top of form dialogs: avatar preview,
 * crop-and-upload button and remove button. The cropped image is only kept in
 * memory ({@link #getSelectedFile()}); the owning dialog uploads it on save.
 */
@Slf4j
public class ProfilePhotoSection extends HorizontalLayout {

    private final Icon placeholderIcon = VaadinIcon.USER.create();
    private final Image thumbnail = new Image();
    private final Button removeButton;
    private MultipartFile selectedFile;

    public ProfilePhotoSection(String title, String help, String uploadLabel, String removeLabel) {
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        addClassName(DialogLayout.CLASS_HERO);

        placeholderIcon.addClassName(DialogLayout.CLASS_AVATAR_ICON);

        thumbnail.setWidthFull();
        thumbnail.setHeightFull();
        thumbnail.addClassName(DialogLayout.CLASS_AVATAR_IMG);
        thumbnail.setVisible(false);

        Div avatar = new Div(placeholderIcon, thumbnail);
        avatar.addClassName(DialogLayout.CLASS_AVATAR);

        Span heading = new Span(title);
        heading.addClassName(DialogLayout.CLASS_HERO_TITLE);

        Button uploadButton = new Button(uploadLabel, VaadinIcon.CAMERA.create(), e -> openCropper());
        uploadButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        removeButton = new Button(removeLabel, VaadinIcon.TRASH.create(), e -> clear());
        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeButton.setVisible(false);

        HorizontalLayout actions = new HorizontalLayout(uploadButton, removeButton);
        actions.setPadding(false);
        actions.setSpacing(true);

        VerticalLayout info = new VerticalLayout(heading, DialogLayout.help(help), actions);
        info.setPadding(false);
        info.setSpacing(false);
        info.setWidthFull();
        info.addClassName(DialogLayout.CLASS_HERO_INFO);

        add(avatar, info);
        expand(info);
    }

    /** Shows an already stored picture (URL or data URI); blank values are ignored. */
    public void showExisting(String src) {
        if (src != null && !src.isBlank()) {
            display(src);
        }
    }

    /** The newly cropped picture waiting to be uploaded, or {@code null}. */
    public MultipartFile getSelectedFile() {
        return selectedFile;
    }

    /** Drops the pending picture and goes back to the placeholder. */
    public void clear() {
        selectedFile = null;
        thumbnail.setVisible(false);
        placeholderIcon.setVisible(true);
        removeButton.setVisible(false);
    }

    private void openCropper() {
        new ImageCropperDialog(cropped -> {
            if (cropped != null) {
                selectedFile = cropped;
                preview(cropped);
            }
        }).open();
    }

    private void preview(MultipartFile file) {
        try {
            display("data:" + file.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(file.getBytes()));
        } catch (Exception ex) {
            log.warn("Failed to render image thumbnail", ex);
        }
    }

    private void display(String src) {
        thumbnail.setSrc(src);
        thumbnail.setVisible(true);
        placeholderIcon.setVisible(false);
        removeButton.setVisible(true);
    }
}
