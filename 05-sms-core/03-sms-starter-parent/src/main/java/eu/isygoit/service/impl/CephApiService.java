package eu.isygoit.service.impl;


import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.exception.MinIoObjectException;
import eu.isygoit.enums.IEnumLogicalOpe;
import eu.isygoit.model.FileStorage;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.service.ICephApiService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import io.minio.messages.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


/**
 * The type Ceph api service.
 */
@Slf4j
@Service
@Transactional
public class CephApiService implements ICephApiService {

    @Autowired
    private Map<String, MinioClient> minIoMap;

    /**
     * Gets connection.
     *
     * @param config the config
     * @return the connection
     */
    public MinioClient getConnection(StorageConfig config) {
        if (minIoMap.containsKey(config.getDomain())) {
            return minIoMap.get(config.getDomain());
        }
        MinioClient minioClient = new MinioClient.Builder().credentials(config.getUserName(),
                        config.getPassword())
                .endpoint(config.getUrl()).build();
        minIoMap.put(config.getDomain(), minioClient);
        return minioClient;
    }

    private boolean bucketExist(StorageConfig config, String bucketName) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            return minioClientConnection.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public void makeBucket(StorageConfig config, String bucketName) {
        try {
            if (!this.bucketExist(config, bucketName)) {
                MinioClient minioClientConnection = this.getConnection(config);
                minioClientConnection.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public void uploadFile(StorageConfig config, String bucketName, String objectName, MultipartFile multipartFile, Map<String, String> tag) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            if (!this.bucketExist(config, bucketName)) {
                this.makeBucket(config, bucketName);
            }

            minioClientConnection.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .tags(tag)
                    .contentType(multipartFile.getContentType())
                    .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                    .build());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when upload file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public byte[] getObject(StorageConfig config, String bucketName, String objectName) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            InputStream stream = minioClientConnection.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return IOUtils.toByteArray(stream);
        } catch (Throwable e) {
            log.error("<Error>: Happened error when get list objects from minio: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public String getPresignedObjectUrl(StorageConfig config, String bucketName, String objectName) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            return minioClientConnection.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(2, TimeUnit.HOURS)
                    .build());
        } catch (Throwable e) {
            log.error("<Error>: ErrorResponseException, message : {}", e.getMessage());
            throw new MinIoObjectException(e);
        }
    }

    public void deleteObject(StorageConfig config, String bucketName, String objectName) {

        try {
            MinioClient minioClientConnection = this.getConnection(config);
            minioClientConnection.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public List<FileStorage> getObjectByTags(StorageConfig config, String bucketName, Map<String, String> tags, IEnumLogicalOpe.Types condition) {
        List<FileStorage> allObjects = this.getObjects(config, bucketName);
        return allObjects.stream().map(fileStorage -> {
            boolean accepted = false;
            try {
                MinioClient minioClientConnection = this.getConnection(config);
                Tags tagsList = minioClientConnection.getObjectTags(GetObjectTagsArgs.builder().
                        bucket(bucketName).
                        object(fileStorage.getObjectName())
                        .build());


                if (condition == IEnumLogicalOpe.Types.AND) {
                    accepted = tagsList.get().values().containsAll(tags.values());
                    fileStorage.setTags(tags.values().stream().toList());
                } else {
                    accepted = !Collections.disjoint(tagsList.get().values(), tags.values());
                    fileStorage.setTags(tagsList.get().values().stream()
                            .distinct()
                            .filter(tags.values()::contains)
                            .toList());
                }
            } catch (Throwable e) {
                log.error("<Error>: Happened error when get list objects from minio: {} ", e);
                throw new MinIoObjectException(e);
            }
            if (accepted) {
                return fileStorage;
            }
            return null;
        }).toList();
    }

    public List<FileStorage> getObjects(StorageConfig config, String bucketName) {
        MinioClient minioClientConnection = this.getConnection(config);
        Iterable<Result<Item>> results = minioClientConnection.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build());

        return StreamSupport.stream(results.spliterator(), true)
                .map(itemResult -> {
                    try {
                        Item item = itemResult.get();
                        return FileStorage.builder()
                                .objectName(item.objectName())
                                .size(item.size())
                                .etag(item.etag())
                                .lastModified(item.lastModified())
                                .build();
                    } catch (Throwable e) {
                        log.error("<Error>: Happened error when get list objects from minio: {} ", e);
                        throw new MinIoObjectException(e);
                    }
                }).collect(Collectors.toList());
    }

    public void updateTags(StorageConfig config, String bucketName, String objectName, Map<String, String> tags) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            minioClientConnection.setObjectTags(
                    SetObjectTagsArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .tags(tags)
                            .build());

        } catch (Throwable e) {
            log.error("<Error>: Happened error when get list objects from minio: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public void deleteObjects(StorageConfig config, String bucketName, List<DeleteObject> objects) {
        MinioClient minioClientConnection = this.getConnection(config);
        Iterable<Result<DeleteError>> results = minioClientConnection.removeObjects(
                RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build());

        StreamSupport.stream(results.spliterator(), false).forEach(deleteErrorResult -> {
            try {
                DeleteError error = deleteErrorResult.get();
                System.out.println(
                        "Error in deleting object " + error.objectName() + "; " + error.message());
            } catch (Throwable e) {
                log.error("<Error>: Happened error when removing file: {} ", e);
                throw new MinIoObjectException(e);
            }
        });
    }

    public void deletebucket(StorageConfig config, String bucketName) {

        try {
            MinioClient minioClientConnection = this.getConnection(config);
            if (bucketExist(config, bucketName)) {
                minioClientConnection.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
            }

        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public List<BucketDto> getBuckets(StorageConfig config) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            return minioClientConnection.listBuckets().stream().map(b -> BucketDto.builder()
                    .name(b.name())
                    .creationDate(b.creationDate().toLocalDateTime())
                    .build()).collect(Collectors.toList());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

}
