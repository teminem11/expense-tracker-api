package com.artem.expensetracker.auth;

import com.artem.expensetracker.common.ApiException;
import com.artem.expensetracker.security.JwtService;
import com.artem.expensetracker.user.User;
import com.artem.expensetracker.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    record RegisterRequest(@NotBlank @Size(max = 100) String name, @Email @NotBlank String email,
                           @Size(min = 8, max = 72) String password) {
    }

    record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    record AuthResponse(String token, Long userId, String name, String email) {
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        if (users.existsByEmailIgnoreCase(r.email()))
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPassword(encoder.encode(r.password()));
        users.save(u);
        return response(u);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPassword()))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return response(u);
    }

    private AuthResponse response(User u) {
        return new AuthResponse(jwt.generate(u.getEmail()), u.getId(), u.getName(), u.getEmail());
    }
}
