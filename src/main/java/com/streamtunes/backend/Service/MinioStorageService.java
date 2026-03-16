package com.streamtunes.backend.Service;

import com.streamtunes.backend.Config.MinioConfig;
import com.streamtunes.backend.Repository.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig config;

    public MinioStorageService(MinioClient minioClient, MinioConfig config) {
        this.minioClient = minioClient;
        this.config = config;
    }

    @Override
    public void upload(String key, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .stream(stream, size, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public InputStream downloadRange(String key, long offset, long length) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .offset(offset)
                            .length(length)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file range", e);
        }
    }

    @Override
    public long getObjectSize(String key) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(key)
                            .build()
            ).size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
