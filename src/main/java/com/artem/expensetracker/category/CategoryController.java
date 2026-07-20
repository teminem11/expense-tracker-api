package com.artem.expensetracker.category;

import com.artem.expensetracker.common.ApiException;
import com.artem.expensetracker.expense.ExpenseRepository;
import com.artem.expensetracker.user.CurrentUserService;
import com.artem.expensetracker.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryRepository repo;
    private final ExpenseRepository expenses;
    private final CurrentUserService current;

    record Request(@NotBlank @Size(max = 80) String name, @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String color) {
    }

    record Response(Long id, String name, String color) {
    }

    @GetMapping
    List<Response> all() {
        return repo.findAllByUserIdOrderByName(current.get().getId()).stream().map(this::dto).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response create(@Valid @RequestBody Request r) {
        User u = current.get();
        if (repo.existsByUserIdAndNameIgnoreCase(u.getId(), r.name()))
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists");
        Category c = new Category();
        c.setName(r.name().trim());
        c.setColor(r.color());
        c.setUser(u);
        return dto(repo.save(c));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        Long userId = current.get().getId();
        Category c = repo.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (expenses.existsByCategoryIdAndUserId(id, userId)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category is used by one or more expenses");
        }
        repo.delete(c);
    }

    private Response dto(Category c) {
        return new Response(c.getId(), c.getName(), c.getColor());
    }
}
