package eu.isygoit.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * The type File storage.
 */

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class FileStorage {

    /**
     * The Object name.
     */
    private String objectName;
    /**
     * The Size.
     */
    private long size;
    /**
     * The Etag.
     */
    private String etag;
    /**
     * The Last modified.
     */
    private ZonedDateTime lastModified;
    /**
     * The Tags.
     */
    private List<String> tags;

    /**
     * The Version id.
     */
    private String versionID;

    /**
     * The Current version.
     */
    private boolean currentVersion;
}


