package eu.isygoit.ui.common.dialog;

/**
 * Upload limits shared by every dialog that accepts documents.
 */
public final class DialogUploads {

    /** Maximum size of one uploaded document (20 MB). */
    public static final int MAX_FILE_BYTES = 20 * 1024 * 1024;

    /** Documents and images accepted as attachments. */
    public static final String[] DOCUMENT_MIME_TYPES = {
            "application/pdf",
            "image/*",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    };

    private DialogUploads() {
    }
}
