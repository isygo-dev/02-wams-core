package eu.isygoit.service.impl;


import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.exception.MinIoObjectException;
import eu.isygoit.enums.IEnumLogicalOpe;
import eu.isygoit.model.FileStorage;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.service.IMinIOApiService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * The type Min io api service.
 */
@Slf4j
@Service
@Transactional
public class MinIOApiService implements IMinIOApiService {

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

    public void updateConnection(StorageConfig config) {
        if (minIoMap.containsKey(config.getDomain())) {
            MinioClient minioClient = new MinioClient.Builder().credentials(config.getUserName(),
                            config.getPassword())
                    .endpoint(config.getUrl()).build();
            minIoMap.put(config.getDomain(), minioClient);
        }
    }

    private boolean bucketExist(StorageConfig config, String bucketName) {
        try {
            log.info("before connection");
            log.info("connection " + this.getConnection(config));
            MinioClient minioClientConnection = this.getConnection(config);
            log.info("after connection");
            return minioClientConnection.bucketExists(
                    BucketExistsArgs
                            .builder().
                            bucket(bucketName)
                            .build());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }


    public void setVersioningBucket(StorageConfig config, String bucketName, boolean status) {
        try {
            VersioningConfiguration configBucket;
            if (status) {

                configBucket = new VersioningConfiguration(VersioningConfiguration.Status.ENABLED, true);

            } else {
                configBucket = new VersioningConfiguration(VersioningConfiguration.Status.SUSPENDED, true);

            }
            MinioClient minioClientConnection = this.getConnection(config);
            minioClientConnection.setBucketVersioning(
                    SetBucketVersioningArgs.builder().bucket(bucketName).config(configBucket).build());

        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public void makeBucket(StorageConfig config, String bucketName) {
        try {
            log.info(config.toString());
            log.info("bucketName" + bucketName);
            log.info("test connection");
            log.info("!this.bucketExist(config, bucketName)" + !this.bucketExist(config, bucketName));
            if (!this.bucketExist(config, bucketName)) {
                log.info("bucketExist okk");
                MinioClient minioClientConnection = this.getConnection(config);
                log.info("minioClientConnection" + minioClientConnection);
                minioClientConnection.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public void uploadFile(StorageConfig config, String bucketName, String path, String objectName, MultipartFile multipartFile, Map<String, String> tag) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            log.info(config.toString());
            log.info(bucketName);
            if (!this.bucketExist(config, bucketName)) {
                this.makeBucket(config, bucketName);
            }

            minioClientConnection.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path + "/" + objectName)
                    .tags(tag)
                    .contentType(multipartFile.getContentType())
                    .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                    .build());
        } catch (Throwable e) {
            log.error("<Error>: Happened error when upload file: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public byte[] getObject(StorageConfig config, String bucketName, String objectName, String versionID) {
        try {
            log.info("config" + config.toString());
            log.info("bucketName" + bucketName);
            log.info("objectName" + objectName);
            MinioClient minioClientConnection = this.getConnection(config);
            InputStream stream = minioClientConnection.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .versionId(versionID)
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
        try {
            List<FileStorage> listFileStorage = new ArrayList<>();
            List<FileStorage> allObject = this.getObjects(config, bucketName);

            for (FileStorage object : allObject) {
                MinioClient minioClientConnection = this.getConnection(config);
                Tags tagsList = minioClientConnection.getObjectTags(GetObjectTagsArgs.builder().
                        bucket(bucketName).
                        object(object.objectName)
                        .build());

                boolean accepted = false;
                if (condition == IEnumLogicalOpe.Types.AND) {
                    accepted = tagsList.get().values().containsAll(tags.values());
                    object.tags = tags.values().stream().toList();
                } else {
                    accepted = !Collections.disjoint(tagsList.get().values(), tags.values());
                    object.tags = tagsList.get().values().stream()
                            .distinct()
                            .filter(tags.values()::contains)
                            .toList();
                }

                if (accepted) {
                    listFileStorage.add(object);
                }
            }
            return listFileStorage;

        } catch (Throwable e) {
            log.error("<Error>: Happened error when get list objects from minio: {} ", e);
            throw new MinIoObjectException(e);
        }
    }

    public List<FileStorage> getObjects(StorageConfig config, String bucketName) {
        try {
            MinioClient minioClientConnection = this.getConnection(config);
            Iterable<Result<Item>> results = minioClientConnection.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .includeVersions(true)
                    .recursive(true)
                    .build());
            List<FileStorage> listFileStorage = new ArrayList<>();
            for (Result<Item> result : results) {
                FileStorage fileObject = new FileStorage();
                Item item = result.get();
                fileObject.objectName = item.objectName();
                fileObject.size = item.size();
                fileObject.etag = item.etag();
                fileObject.lastModified = item.lastModified();

                fileObject.versionID = item.versionId();
                fileObject.currentVersion = item.isLatest();

                listFileStorage.add(fileObject);
            }
            return listFileStorage;

        } catch (Throwable e) {
            log.error("<Error>: Happened error when get list objects from minio: {} ", e);
            throw new MinIoObjectException(e);
        }
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

        try {
            MinioClient minioClientConnection = this.getConnection(config);
            Iterable<Result<DeleteError>> results = minioClientConnection.removeObjects(
                    RemoveObjectsArgs.builder().bucket(bucketName).objects(objects).build());

            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                System.out.println(
                        "Error in deleting object " + error.objectName() + "; " + error.message());
            }
        } catch (Throwable e) {
            log.error("<Error>: Happened error when removing file: {} ", e);
            throw new MinIoObjectException(e);
        }
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
