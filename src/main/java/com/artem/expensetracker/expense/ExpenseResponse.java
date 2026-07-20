package com.artem.expensetracker.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        BigDecimal amount,
        String description,
        LocalDate expenseDate,
        Long categoryId,
        String categoryName,
        boolean receiptAvailable,
        String receiptUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
