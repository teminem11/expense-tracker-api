package com.artem.expensetracker.expense;

import com.artem.expensetracker.category.Category;
import com.artem.expensetracker.category.CategoryRepository;
import com.artem.expensetracker.common.ApiException;
import com.artem.expensetracker.storage.StorageService;
import com.artem.expensetracker.user.CurrentUserService;
import com.artem.expensetracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    @Mock ExpenseRepository expenses;
    @Mock CategoryRepository categories;
    @Mock CurrentUserService currentUser;
    @Mock StorageService storage;

    private ExpenseService service;
    private User user;
    private Category category;
    private Expense expense;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(expenses, categories, currentUser, storage);
        user = new User();
        user.setId(7L);
        category = new Category();
        category.setId(3L);
        category.setName("Food");
        category.setUser(user);
        expense = new Expense();
        expense.setId(11L);
        expense.setAmount(new BigDecimal("12.50"));
        expense.setDescription("Lunch");
        expense.setExpenseDate(LocalDate.of(2026, 7, 20));
        expense.setCategory(category);
        expense.setUser(user);
    }

    @Test
    void updatesOnlyExpenseOwnedByCurrentUser() {
        when(currentUser.get()).thenReturn(user);
        when(expenses.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(expense));
        when(categories.findByIdAndUserId(3L, 7L)).thenReturn(Optional.of(category));

        ExpenseResponse response = service.update(11L, new ExpenseRequest(
                new BigDecimal("15.99"), " Dinner ", LocalDate.of(2026, 7, 21), 3L));

        assertThat(response.amount()).isEqualByComparingTo("15.99");
        assertThat(response.description()).isEqualTo("Dinner");
        assertThat(response.categoryId()).isEqualTo(3L);
    }

    @Test
    void hidesExpenseOwnedByAnotherUser() {
        when(currentUser.get()).thenReturn(user);
        when(expenses.findByIdAndUserId(99L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void storesNewReceiptBeforeDeletingPreviousOne() {
        expense.setReceiptKey("receipts/7/old.png");
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1});
        when(currentUser.get()).thenReturn(user);
        when(expenses.findByIdAndUserId(11L, 7L)).thenReturn(Optional.of(expense));
        when(storage.store(7L, file)).thenReturn("receipts/7/new.png");

        ExpenseResponse response = service.uploadReceipt(11L, file);

        InOrder order = inOrder(storage);
        order.verify(storage).store(7L, file);
        order.verify(storage).delete("receipts/7/old.png");
        assertThat(response.receiptUrl()).isEqualTo("/api/expenses/11/receipt");
    }

    @Test
    void rejectsNonImageWithoutCallingStorage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "receipt.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[]{1});

        assertThatThrownBy(() -> service.uploadReceipt(11L, file))
                .isInstanceOf(ApiException.class)
                .hasMessage("Receipt must be a non-empty image");
        verify(storage, never()).store(7L, file);
    }
}
