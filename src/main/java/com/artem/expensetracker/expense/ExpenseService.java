package com.artem.expensetracker.expense;

import com.artem.expensetracker.category.Category;
import com.artem.expensetracker.category.CategoryRepository;
import com.artem.expensetracker.common.ApiException;
import com.artem.expensetracker.storage.ReceiptContent;
import com.artem.expensetracker.storage.StorageService;
import com.artem.expensetracker.user.CurrentUserService;
import com.artem.expensetracker.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final CategoryRepository categories;
    private final CurrentUserService currentUser;
    private final StorageService storage;

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> findAll(LocalDate from, LocalDate to, Pageable pageable) {
        if (from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The 'from' date must not be after 'to'");
        }
        return expenses.findAllByUserIdAndExpenseDateBetween(currentUser.get().getId(), from, to, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse findById(Long id) {
        return toResponse(findOwned(id));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        User user = currentUser.get();
        Category category = findOwnedCategory(request.categoryId(), user.getId());
        Expense expense = new Expense();
        expense.setUser(user);
        apply(expense, request, category);
        return toResponse(expenses.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOwned(id);
        Category category = findOwnedCategory(request.categoryId(), expense.getUser().getId());
        apply(expense, request, category);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse uploadReceipt(Long id, MultipartFile file) {
        validateReceipt(file);
        Expense expense = findOwned(id);
        String newKey = storage.store(expense.getUser().getId(), file);
        String oldKey = expense.getReceiptKey();
        expense.setReceiptKey(newKey);
        if (oldKey != null) {
            storage.delete(oldKey);
        }
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public ReceiptContent downloadReceipt(Long id) {
        Expense expense = findOwned(id);
        if (expense.getReceiptKey() == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Receipt not found");
        }
        return storage.load(expense.getReceiptKey());
    }

    @Transactional
    public void delete(Long id) {
        Expense expense = findOwned(id);
        String receiptKey = expense.getReceiptKey();
        expenses.delete(expense);
        if (receiptKey != null) {
            storage.delete(receiptKey);
        }
    }

    private Expense findOwned(Long id) {
        return expenses.findByIdAndUserId(id, currentUser.get().getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private Category findOwnedCategory(Long categoryId, Long userId) {
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private void apply(Expense expense, ExpenseRequest request, Category category) {
        expense.setAmount(request.amount());
        expense.setDescription(request.description().trim());
        expense.setExpenseDate(request.expenseDate());
        expense.setCategory(category);
    }

    private void validateReceipt(MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Receipt must be a non-empty image");
        }
    }

    private ExpenseResponse toResponse(Expense expense) {
        boolean hasReceipt = expense.getReceiptKey() != null;
        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getExpenseDate(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                hasReceipt,
                hasReceipt ? "/api/expenses/" + expense.getId() + "/receipt" : null,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
