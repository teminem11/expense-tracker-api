package com.artem.expensetracker.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    @EntityGraph(attributePaths = {"category", "user"})
    Page<Expense> findAllByUserIdAndExpenseDateBetween(Long userId, LocalDate from, LocalDate to, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "user"})
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryIdAndUserId(Long categoryId, Long userId);
}
