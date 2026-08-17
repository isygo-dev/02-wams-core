package eu.isygoit.service.impl;

import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.exception.MinIoObjectException;
import eu.isygoit.enums.IEnumLogicalOperator;
import eu.isygoit.mapper.FileStorageMapper;
import eu.isygoit.mapper.S3ConfigMapper;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.s3.config.S3Config;
import eu.isygoit.s3.object.FileStorage;
import eu.isygoit.s3.object.MetaData;
import eu.isygoit.service.ISmsMinIOApiService;
import eu.isygoit.service.IObjectStorageService;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import eu.isygoit.dto.data.FileStorageDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Min io storage service.
 */
@Slf4j
@Service("MinIOStorageService")
@Transactional
public class MinIOStorageService implements IObjectStorageService {

    @Autowired
    private ISmsMinIOApiService minioService;

    @Autowired
    private S3ConfigMapper s3ConfigMapper;

    @Autowired
    private FileStorageMapper fileStorageMapper;

    @Override
    public void upload(StorageConfig config, String bucketName, String path, Map<String, String> tags, MultipartFile multipartFile) {
        try {
            log.info("Uploading file to bucket: {}, path: {}, fileName: {}", bucketName, path, multipartFile.getOriginalFilename());

            // Build MetaData
            MetaData metaData = MetaData.builder()
                    .bucketName(bucketName)
                    .path(path)
                    .objectName(multipartFile.getOriginalFilename())
                    .tagsMap(tags)
                    .contentType(multipartFile.getContentType())
                    .build();

            // Convert StorageConfig to S3Config (assuming mapper)
            S3Config s3Config = s3ConfigMapper.entityToDto(config);

            // Perform upload
            MetaData result = minioService.uploadFile(s3Config, metaData, multipartFile);

            log.info("Upload completed. ETag: {}, VersionID: {}", result.getEtag(), result.getVersionID());
        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public byte[] download(StorageConfig config, String bucketName, String fileName, String versionID) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            return minioService.getObject(s3Config, bucketName.toLowerCase(), fileName, versionID);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deleteFile(StorageConfig config, String bucketName, String fileName) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.deleteObject(s3Config, bucketName.toLowerCase(), fileName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void updateTags(StorageConfig config, String bucketName, String objectName, Map<String, String> tags) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.updateTags(s3Config, bucketName.toLowerCase(), objectName, tags);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<FileStorageDto> getObjectByTags(StorageConfig config, String bucketName,
                                                Map<String, String> tags, IEnumLogicalOperator.Types condition) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            String safeBucketName = bucketName != null ? bucketName.toLowerCase() : null;
            List<FileStorage> fileStorageList = minioService.getObjectByTags(s3Config, safeBucketName, tags, condition);
            return fileStorageList.stream()
                    .map(fileStorageMapper::entityToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<FileStorageDto> getObjects(StorageConfig config, String bucketName) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            List<FileStorage> fileStorageList =  minioService.getObjects(s3Config, bucketName.toLowerCase());
            return fileStorageList.stream()
                    .map(fileStorageMapper::entityToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deleteObjects(StorageConfig config, String bucketName, List<DeleteObject> objects) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.deleteObjects(s3Config, bucketName.toLowerCase(), objects);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void saveBuckets(StorageConfig config, String bucketName) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.makeBucket(s3Config, bucketName.toLowerCase());
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void setVersioningBucket(StorageConfig config, String bucketName, boolean status) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.setVersioningBucket(s3Config, bucketName.toLowerCase(), status);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deletebucket(StorageConfig config, String bucketName) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            minioService.deleteBucket(s3Config, bucketName.toLowerCase());
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<BucketDto> getBuckets(StorageConfig config) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            List<Bucket> buckets = minioService.getBuckets(s3Config);
            return buckets.stream().map(bucket -> BucketDto.builder()
                    .name(bucket.name())
                    .creationDate(bucket.creationDate().toLocalDateTime())
                    //.bucketRegion(bucket.region())
                    //.bucketArn(bucket.arn())
                    .build())
            .collect(Collectors.toList());
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public MetaData getMetaData(StorageConfig config, String lowerCase, String objectName, String versionID) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            return minioService.getMetaData(s3Config, lowerCase, objectName, versionID);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public String getPresignedUrl(StorageConfig config, String lowerCase, String objectName) {
        try {
            S3Config s3Config = s3ConfigMapper.entityToDto(config);
            return minioService.getPresignedObjectUrl(s3Config, lowerCase, objectName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }
}
