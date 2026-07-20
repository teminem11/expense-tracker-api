package com.artem.expensetracker.storage;

import org.springframework.http.MediaType;

public record ReceiptContent(byte[] bytes, MediaType contentType, String filename) {
}
