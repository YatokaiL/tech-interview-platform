package com.diploma.report_service.storage;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.PutObjectArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class MinioStorage {

    private final MinioClient client;
    private final String bucket;

    public MinioStorage(MinioClient client, @Value("${app.minio.bucket}") String bucket) {
        this.client = client;
        this.bucket = bucket;
    }

    public void upload(String objectKey, byte[] body, String contentType) {
        try (InputStream in = new ByteArrayInputStream(body)) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(in, body.length, -1)
                    .build());
            log.debug("Uploaded {} bytes to {}/{}", body.length, bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("MinIO upload failed for " + objectKey, e);
        }
    }

    public InputStream download(String objectKey) {
        try {
            GetObjectResponse response = client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            return response;
        } catch (Exception e) {
            throw new RuntimeException("MinIO download failed for " + objectKey, e);
        }
    }

    public byte[] downloadBytes(String objectKey) {
        try (InputStream in = download(objectKey)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("MinIO read failed for " + objectKey, e);
        }
    }
}
