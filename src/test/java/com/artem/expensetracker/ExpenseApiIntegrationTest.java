package com.artem.expensetracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void authenticatedUserCanManageOwnExpenseButCannotSeeAnotherUsersExpense() throws Exception {
        String ownerToken = register("Owner", "owner@example.com");
        long categoryId = createCategory(ownerToken, "Food");
        long expenseId = createExpense(ownerToken, categoryId);

        mvc.perform(put("/api/expenses/{id}", expenseId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 15.99,
                                  "description": "Dinner",
                                  "expenseDate": "2026-07-21",
                                  "categoryId": %d
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(15.99))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.categoryName").value("Food"));

        mvc.perform(get("/api/expenses/{id}", expenseId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptAvailable").value(false))
                .andExpect(jsonPath("$.receiptUrl").doesNotExist());

        String otherToken = register("Other", "other@example.com");
        mvc.perform(get("/api/expenses/{id}", expenseId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedEndpointReturnsUnauthorizedWithoutToken() throws Exception {
        mvc.perform(get("/api/expenses"))
                .andExpect(status().isUnauthorized());
    }

    private String register(String name, String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"StrongPass123"}
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private long createCategory(String token, String name) throws Exception {
        String response = mvc.perform(post("/api/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","color":"#22C55E"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private long createExpense(String token, long categoryId) throws Exception {
        String response = mvc.perform(post("/api/expenses")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount": 12.50,
                                  "description": "Lunch",
                                  "expenseDate": "2026-07-20",
                                  "categoryId": %d
                                }
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        return body.get("id").asLong();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
