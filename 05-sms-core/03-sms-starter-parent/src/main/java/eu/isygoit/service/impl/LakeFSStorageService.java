package eu.isygoit.service.impl;

import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.exception.MinIoObjectException;
import eu.isygoit.enums.IEnumLogicalOpe;
import eu.isygoit.model.FileStorage;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.service.ILakeFSApiService;
import eu.isygoit.service.IObjectStorageService;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * The type Lake fs storage service.
 */
@Slf4j
@Service("LakeFSStorageService")
@Transactional
public class LakeFSStorageService implements IObjectStorageService {

    @Autowired
    private ILakeFSApiService lakeFSApiService;

    @Override
    public void upload(StorageConfig config, String bucketName, String path, Map<String, String> tags, MultipartFile multipartFile) {
        try {

            lakeFSApiService.uploadFile(config, bucketName, multipartFile.getOriginalFilename(), multipartFile, tags);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<FileStorage> getObjects(StorageConfig config, String bucketName) {
        try {
            return lakeFSApiService.getObjects(config, bucketName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public byte[] download(StorageConfig config, String bucketName, String fileName, String versionID) {
        try {
            return lakeFSApiService.getObject(config, bucketName, fileName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deleteFile(StorageConfig config, String bucketName, String fileName) {
        try {
            lakeFSApiService.deleteObject(config, bucketName, fileName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void updateTags(StorageConfig config, String bucketName, String objectName, Map<String, String> tags) {
        try {
            lakeFSApiService.updateTags(config, bucketName, objectName, tags);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<FileStorage> getObjectByTags(StorageConfig config, String bucketName, Map<String, String> tags, IEnumLogicalOpe.Types condition) {
        try {
            return lakeFSApiService.getObjectByTags(config, bucketName, tags, condition);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deleteObjects(StorageConfig config, String bucketName, List<DeleteObject> objects) {
        try {
            lakeFSApiService.deleteObjects(config, bucketName, objects);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void saveBuckets(StorageConfig config, String bucketName) {
        try {
            lakeFSApiService.makeBucket(config, bucketName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void setVersioningBucket(StorageConfig config, String bucketName, boolean status) {
        try {
            lakeFSApiService.makeBucket(config, bucketName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public void deletebucket(StorageConfig config, String bucketName) {
        try {
            lakeFSApiService.deletebucket(config, bucketName);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }

    @Override
    public List<BucketDto> getBuckets(StorageConfig config) {
        try {
            return lakeFSApiService.getBuckets(config);
        } catch (Exception e) {
            throw new MinIoObjectException(e);
        }
    }
}
