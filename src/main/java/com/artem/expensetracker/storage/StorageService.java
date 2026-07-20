package com.artem.expensetracker.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String store(Long userId, MultipartFile file);

    ReceiptContent load(String key);

    void delete(String key);
}
