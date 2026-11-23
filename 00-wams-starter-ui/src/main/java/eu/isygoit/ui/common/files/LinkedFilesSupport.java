package eu.isygoit.ui.common.files;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import eu.isygoit.dto.common.LinkedFileDto;
import eu.isygoit.exception.OperationNotSupportedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

/**
 * Shared helpers for dialogs that manage linked files.
 *
 * <p>Extracted from the Budget and AcademicPeriod modules to avoid duplicating
 * the file-icon mapping, the byte-size formatter and the two MultipartFile
 * adapters in every dialog.</p>
 */
public final class LinkedFilesSupport {

    private LinkedFilesSupport() {
    }

    /**
     * Maps a MIME type to the Vaadin icon that best represents the file.
     */
    public static Icon getFileIcon(String mimetype) {
        if (mimetype == null) return VaadinIcon.FILE.create();
        if (mimetype.startsWith("image/")) return VaadinIcon.PICTURE.create();
        if (mimetype.equals("application/pdf")) return VaadinIcon.FILE_O.create();
        if (mimetype.contains("word") || mimetype.contains("document")) return VaadinIcon.FILE_TEXT.create();
        if (mimetype.contains("excel") || mimetype.contains("spreadsheet")) return VaadinIcon.TABLE.create();
        return VaadinIcon.FILE.create();
    }

    /**
     * Human-readable byte size ("1.2 KB", "3.4 MB", …).
     */
    public static String formatFileSize(Long size) {
        if (size == null || size <= 0) return "0 B";
        long bytes = size;
        double absBytes = Math.abs(bytes);
        if (absBytes < 1024) return bytes + " B";
        long value = (long) absBytes;
        CharacterIterator ci = new StringCharacterIterator("KMGTPE");
        for (int i = 0; i < 6; i++) {
            value /= 1024;
            if (value < 1024) {
                return String.format(Locale.US, "%.1f %cB",
                        (i == 0 ? (bytes / 1024.0) : (bytes / Math.pow(1024, i + 1))), ci.current());
            }
            ci.next();
        }
        return String.format(Locale.US, "%.1f EB", bytes / Math.pow(1024, 7));
    }

    /**
     * Saves the new category of a stored file. Returns {@code true} when it was saved.
     */
    @FunctionalInterface
    public interface CategoryUpdater {
        boolean update(Long fileId, String category) throws Exception;
    }

    /**
     * File staged locally, uploaded only after the parent has been created / updated.
     */
    public static final class PendingFile {
        public final String name;
        public final String mimeType;
        public final byte[] content;
        /**
         * Category typed in the file row; the caller reads it when it flushes the file.
         */
        public String category;

        public PendingFile(String name, String mimeType, byte[] content) {
            this.name = name;
            this.mimeType = mimeType;
            this.content = content;
        }

        public MultipartFile toMultipartFile() {
            return new ByteArrayMultipartFile(name, name, mimeType, content);
        }

        /**
         * Stores the category typed on this staged file, once the file has been uploaded: the upload answers with
         * the files of the parent, the new one is the most recent with the same name. A failure never blocks the
         * save of the parent, the category can still be edited afterwards.
         *
         * @param uploadResponse answer of the upload call (may be {@code null})
         * @param updater        (fileId, category) {@code ->} the category update call of the parent service
         */
        /**
         * Single-file variant: stores the category typed on this staged file once it has been uploaded.
         *
         * @param updater category {@code ->} the category update call of the parent service
         */
        public void storeSingleCategory(java.util.function.Function<String, ResponseEntity<Boolean>> updater) {
            if (category == null || category.isBlank()) {
                return;
            }
            try {
                updater.apply(category.trim());
            } catch (RuntimeException ex) {
                // the file is stored; only its category is missing
            }
        }

        public void storeCategory(ResponseEntity<? extends List<? extends LinkedFileDto<?>>> uploadResponse,
                                  BiFunction<Long, String, ResponseEntity<Boolean>> updater) {
            if (category == null || category.isBlank() || uploadResponse == null || uploadResponse.getBody() == null) {
                return;
            }
            Long newest = null;
            for (LinkedFileDto<?> file : uploadResponse.getBody()) {
                if (file.getId() instanceof Number id && name.equals(file.getOriginalFileName())
                        && (newest == null || id.longValue() > newest)) {
                    newest = id.longValue();
                }
            }
            if (newest != null) {
                try {
                    updater.apply(newest, category.trim());
                } catch (RuntimeException ex) {
                    // the file is stored; only its category is missing
                }
            }
        }
    }

    /**
     * Minimal in-memory {@link MultipartFile} adapter.
     */
    public static final class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) {
            throw new OperationNotSupportedException("Not implemented");
        }
    }
}