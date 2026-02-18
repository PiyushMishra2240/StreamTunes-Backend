package com.streamtunes.backend.Repository;

import java.io.InputStream;

public interface StorageService {
    void upload(String key, InputStream stream, long size, String contentType);

    InputStream download(String key);

    long getObjectSize(String key);
}
