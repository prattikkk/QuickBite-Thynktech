package com.quickbite.delivery.service;

import com.quickbite.orders.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Local-disk implementation of {@link FileStorageService}.
 * Stores files under a configurable root directory.
 * Replace with S3/MinIO implementation for production.
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/heic"
    );

    @Value("${proof.upload-dir:uploads/proofs}")
    private String uploadDir;

    private Path rootLocation;

    @PostConstruct
    void init() {
        rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("File storage root: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory: " + rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("Unsupported file type: " + contentType
                    + ". Allowed: JPEG, PNG, WebP, HEIC");
        }

        String extension = getExtension(file.getOriginalFilename(), contentType);
        String filename = UUID.randomUUID() + extension;

        Path targetDir = rootLocation.resolve(subDirectory).normalize();
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();

            // Ensure target is still within root (prevent path traversal)
            if (!target.startsWith(rootLocation)) {
                throw new BusinessException("Invalid storage path");
            }

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} ({} bytes)", target, file.getSize());

            // Return relative URL path
            return "/uploads/proofs/" + subDirectory + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file", e);
            throw new BusinessException("Failed to store file: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) return;
        try {
            // Strip leading /uploads/proofs/ prefix to get relative path
            String relative = filePath.replaceFirst("^/uploads/proofs/", "");
            Path target = rootLocation.resolve(relative).normalize();
            if (target.startsWith(rootLocation)) {
                Files.deleteIfExists(target);
                log.info("Deleted file: {}", target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", filePath, e.getMessage());
        }
    }

    private String getExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            default -> ".jpg";
        };
    }
}
