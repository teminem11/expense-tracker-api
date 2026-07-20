package com.artem.expensetracker.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 255) String description,
        @NotNull LocalDate expenseDate,
        @NotNull Long categoryId
) {
}
