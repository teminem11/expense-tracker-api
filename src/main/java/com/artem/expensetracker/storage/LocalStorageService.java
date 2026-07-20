package com.artem.expensetracker.storage;

import com.artem.expensetracker.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
    private static final String RECEIPTS_PREFIX = "receipts/";

    private final Path root;

    public LocalStorageService(@Value("${app.storage.local-directory}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(Long userId, MultipartFile file) {
        String key = RECEIPTS_PREFIX + userId + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        Path target = resolveKey(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
            return key;
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store receipt");
        }
    }

    @Override
    public ReceiptContent load(String key) {
        Path source = resolveStoredPath(key);
        if (!Files.isRegularFile(source)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Receipt file not found");
        }
        try {
            MediaType contentType = MediaTypeFactory.getMediaType(source.getFileName().toString())
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);
            return new ReceiptContent(Files.readAllBytes(source), contentType, source.getFileName().toString());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read receipt");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveStoredPath(key));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not delete receipt");
        }
    }

    private Path resolveStoredPath(String key) {
        // Backward compatibility for receipts stored before relative keys were introduced.
        Path legacy = Path.of(key);
        if (legacy.isAbsolute()) {
            Path normalized = legacy.normalize();
            if (!normalized.startsWith(root)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid receipt key");
            }
            return normalized;
        }
        return resolveKey(key);
    }

    private Path resolveKey(String key) {
        String normalizedKey = key.replace('\\', '/');
        if (!normalizedKey.startsWith(RECEIPTS_PREFIX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid receipt key");
        }
        Path resolved = root.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid receipt key");
        }
        return resolved;
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        String extension = name.substring(name.lastIndexOf('.')).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
