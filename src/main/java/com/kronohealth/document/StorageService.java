package com.kronohealth.document;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO (S3-compatible) fájl storage service.
 * Object key format: {userId}/{documentId}/{originalFileName}
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${app.s3.bucket}")
    private String bucket;

    /**
     * Upload fájl MinIO-ba.
     * @return az S3 object key
     */
    public String upload(UUID userId, UUID documentId, MultipartFile file) {
        String objectKey = buildKey(userId, documentId, file.getOriginalFilename());

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.debug("Uploaded to S3: {}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("S3 upload failed for key '{}': {}", objectKey, e.getMessage());
            throw new RuntimeException("Fájl feltöltés sikertelen (S3)", e);
        }
    }

    /**
     * Download fájl MinIO-ból.
     * @return InputStream a fájl tartalmával
     */
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("S3 download failed for key '{}': {}", objectKey, e.getMessage());
            throw new RuntimeException("Fájl letöltés sikertelen (S3)", e);
        }
    }

    /**
     * Fájl törlése MinIO-ból.
     */
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.debug("Deleted from S3: {}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.error("S3 delete failed for key '{}': {}", objectKey, e.getMessage());
            throw new RuntimeException("Fájl törlés sikertelen (S3)", e);
        }
    }

    private String buildKey(UUID userId, UUID documentId, String originalFileName) {
        return userId + "/" + documentId + "_" + originalFileName;
    }
}

