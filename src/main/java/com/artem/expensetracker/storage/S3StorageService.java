package com.artem.expensetracker.storage;

import com.artem.expensetracker.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {
    private final S3Client s3;
    private final String bucket;

    public S3StorageService(@Value("${app.aws.region}") String region, @Value("${app.aws.s3-bucket}") String bucket) {
        this.s3 = S3Client.builder().region(software.amazon.awssdk.regions.Region.of(region)).build();
        this.bucket = bucket;
    }

    public String store(Long userId, MultipartFile file) {
        String key = "receipts/" + userId + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(file.getContentType()).build(), RequestBody.fromBytes(file.getBytes()));
            return key;
        } catch (IOException | S3Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not upload receipt to S3");
        }
    }

    @Override
    public ReceiptContent load(String key) {
        validateKey(key);
        try {
            var response = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build());
            String contentType = response.response().contentType();
            MediaType mediaType = contentType == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(contentType);
            return new ReceiptContent(response.asByteArray(), mediaType, key.substring(key.lastIndexOf('/') + 1));
        } catch (NoSuchKeyException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Receipt file not found");
        } catch (S3Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not read receipt from S3");
        }
    }

    public void delete(String key) {
        validateKey(key);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not delete receipt from S3");
        }
    }

    private void validateKey(String key) {
        if (key == null || !key.startsWith("receipts/") || key.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid receipt key");
        }
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) return "";
        String extension = name.substring(name.lastIndexOf('.')).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }
}
