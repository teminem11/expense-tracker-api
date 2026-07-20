package com.artem.expensetracker.storage;

import com.artem.expensetracker.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void storesLoadsAndDeletesReceiptUsingRelativeKey() {
        LocalStorageService storage = new LocalStorageService(tempDirectory.toString());
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        String key = storage.store(42L, file);
        ReceiptContent content = storage.load(key);

        assertThat(key).startsWith("receipts/42/").endsWith(".png");
        assertThat(Path.of(key).isAbsolute()).isFalse();
        assertThat(content.bytes()).containsExactly(1, 2, 3);
        assertThat(content.contentType()).isEqualTo(MediaType.IMAGE_PNG);

        storage.delete(key);
        assertThatThrownBy(() -> storage.load(key))
                .isInstanceOf(ApiException.class)
                .hasMessage("Receipt file not found");
    }

    @Test
    void rejectsKeysOutsideReceiptDirectory() {
        LocalStorageService storage = new LocalStorageService(tempDirectory.toString());

        assertThatThrownBy(() -> storage.load("../secret.txt"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid receipt key");
    }
}
