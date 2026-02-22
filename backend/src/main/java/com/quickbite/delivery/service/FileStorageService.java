package com.quickbite.delivery.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for file storage.
 * Local implementation stores to disk; swap for S3/MinIO in production.
 */
public interface FileStorageService {

    /**
     * Store a file and return its accessible URL path.
     *
     * @param file the multipart file
     * @param subDirectory logical grouping (e.g. "proofs")
     * @return relative URL where the file can be accessed
     */
    String store(MultipartFile file, String subDirectory);

    /**
     * Delete a previously stored file.
     *
     * @param filePath the path returned by {@link #store}
     */
    void delete(String filePath);
}
