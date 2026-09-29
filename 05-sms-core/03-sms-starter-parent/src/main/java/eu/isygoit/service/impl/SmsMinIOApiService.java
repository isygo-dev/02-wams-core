package eu.isygoit.service.impl;


import eu.isygoit.s3.api.impl.MinIOApiService;
import eu.isygoit.service.ISmsMinIOApiService;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;


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
