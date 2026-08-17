package eu.isygoit.service.impl;


import eu.isygoit.dto.data.BucketDto;
import eu.isygoit.dto.exception.MinIoObjectException;
import eu.isygoit.enums.IEnumLogicalOperator;
import eu.isygoit.model.StorageConfig;
import eu.isygoit.s3.api.impl.MinIOApiService;
import eu.isygoit.service.ISmsMinIOApiService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import eu.isygoit.dto.data.FileStorageDto;

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
public class SmsMinIOApiService extends MinIOApiService implements ISmsMinIOApiService {

    public SmsMinIOApiService(Map<String, MinioClient> minIoMap) {
        super(minIoMap);
    }
}
