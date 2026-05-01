package com.karn01.authservice.controller;


import com.karn01.authservice.dto.ApiResponse;
import com.karn01.authservice.dto.AuthTokenResponse;
import com.karn01.authservice.dto.LoginDto;
import com.karn01.authservice.dto.SignupDto;
import com.karn01.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<?> signup(@Valid @RequestBody SignupDto dto) {
        authService.signup(dto);
        return new ApiResponse<>(true, "User signed up successfully", null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody LoginDto dto) {
        String token  = authService.login(dto);
        return new ApiResponse<>(true, "Login successful", new AuthTokenResponse(token));
    }
}
