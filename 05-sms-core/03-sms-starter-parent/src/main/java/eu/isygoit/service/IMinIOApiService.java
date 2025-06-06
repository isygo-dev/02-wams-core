package eu.isygoit.service;


import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.enums.IEnumLogicalOperator;
import eu.isygoit.model.FileStorage;
import eu.isygoit.model.StorageConfig;
import io.minio.messages.DeleteObject;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


/**
 * The interface Min io api service.
 */
public interface IMinIOApiService {

    /**
     * Update connection.
     *
     * @param config the config
     */
    void updateConnection(StorageConfig config);

    /**
     * Upload file.
     *
     * @param config        the config
     * @param bucketName    the bucket name
     * @param path          the path
     * @param objectName    the object name
     * @param multipartFile the multipart file
     * @param tag           the tag
     */
    void uploadFile(StorageConfig config, String bucketName, String path, String objectName, MultipartFile multipartFile, Map<String, String> tag);

    /**
     * Get object byte [ ].
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param objectName the object name
     * @param versionID  the version id
     * @return the byte [ ]
     */
    byte[] getObject(StorageConfig config, String bucketName, String objectName, String versionID);

    /**
     * Gets presigned object url.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param objectName the object name
     * @return the presigned object url
     */
    String getPresignedObjectUrl(StorageConfig config, String bucketName, String objectName);

    /**
     * Delete object.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param objectName the object name
     */
    void deleteObject(StorageConfig config, String bucketName, String objectName);

    /**
     * Gets object by tags.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param tags       the tags
     * @param condition  the condition
     * @return the object by tags
     */
    List<FileStorage> getObjectByTags(StorageConfig config, String bucketName, Map<String, String> tags, IEnumLogicalOperator.Types condition);

    /**
     * Gets objects.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @return the objects
     */
    List<FileStorage> getObjects(StorageConfig config, String bucketName);

    /**
     * Update tags.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param objectName the object name
     * @param tags       the tags
     */
    void updateTags(StorageConfig config, String bucketName, String objectName, Map<String, String> tags);

    /**
     * Delete objects.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param objects    the objects
     */
    void deleteObjects(StorageConfig config, String bucketName, List<DeleteObject> objects);

    /**
     * Sets versioning bucket.
     *
     * @param config     the config
     * @param bucketName the bucket name
     * @param status     the status
     */
    void setVersioningBucket(StorageConfig config, String bucketName, boolean status);

    /**
     * Make bucket.
     *
     * @param config     the config
     * @param bucketName the bucket name
     */
    void makeBucket(StorageConfig config, String bucketName);

    /**
     * Deletebucket.
     *
     * @param config     the config
     * @param bucketName the bucket name
     */
    void deletebucket(StorageConfig config, String bucketName);

    /**
     * Gets buckets.
     *
     * @param config the config
     * @return the buckets
     */
    List<BucketDto> getBuckets(StorageConfig config);
}
