package com.diploma.report_service.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.access-key}")
    private String accessKey;

    @Value("${app.minio.secret-key}")
    private String secretKey;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        return client;
    }

    @Bean
    public BucketInitializer bucketInitializer(MinioClient client) {
        return new BucketInitializer(client, bucket);
    }

    public static class BucketInitializer {
        private final MinioClient client;
        private final String bucket;

        public BucketInitializer(MinioClient client, String bucket) {
            this.client = client;
            this.bucket = bucket;
        }

        @PostConstruct
        public void init() {
            try {
                boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket: {}", bucket);
                } else {
                    log.info("MinIO bucket already exists: {}", bucket);
                }
            } catch (Exception e) {
                log.warn("MinIO bucket initialization failed (will retry on first upload): {}", e.getMessage());
            }
        }
    }
}
