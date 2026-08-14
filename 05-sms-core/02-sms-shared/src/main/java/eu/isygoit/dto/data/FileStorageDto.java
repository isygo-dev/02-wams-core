package eu.isygoit.dto.data;


import java.time.ZonedDateTime;
import java.util.List;

/**
 * The type File storage.
 */
public class FileStorageDto {

    /**
     * The Object name.
     */
    public String objectName;
    /**
     * The Size.
     */
    public long size;
    /**
     * The Etag.
     */
    public String etag;
    /**
     * The Last modified.
     */
    public ZonedDateTime lastModified;
    /**
     * The Tags.
     */
    public List<String> tags;

    /**
     * The Version id.
     */
    public String versionID;

    /**
     * The Current version.
     */
    public boolean currentVersion;
}


