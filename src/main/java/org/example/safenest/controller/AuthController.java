package org.example.safenest.controller;

import jakarta.validation.Valid;
import org.example.safenest.dto.UserRequest;
import org.example.safenest.exception.ApiException;
import org.example.safenest.exception.CustomException;
import org.example.safenest.model.RefreshToken;
import org.example.safenest.model.User;
import org.example.safenest.repository.UserRepository;
import org.example.safenest.security.JwtUtils;
import org.example.safenest.security.UserPrincipal;
import org.example.safenest.service.RefreshTokenService;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshService;
    private final long refreshMs;

    public AuthController(AuthenticationManager authManager,
                          UserRepository userRepo,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          RefreshTokenService refreshService,
                          @org.springframework.beans.factory.annotation.Value("${REFRESH_TOKEN_EXPIRATION}") long refreshMs) {
        this.authManager = authManager;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshService = refreshService;
        this.refreshMs = refreshMs;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid UserRequest userReq) {
        if (userRepo.existsByEmail(userReq.getEmail())) {
            throw new CustomException.UserAlreadyExistsException(userReq.getEmail());
        }
        User u = User.builder()
                .email(userReq.getEmail())
                .passwordHash(passwordEncoder.encode(userReq.getPassword()))
                .kycVerified(false)
                .createdAt(Instant.now())
                .build();
        userRepo.save(u);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserRequest userReq, HttpServletResponse response) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(userReq.getEmail(), userReq.getPassword())
        );
        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        User user = userRepo.findByEmail(up.getUsername())
                .orElseThrow(() -> new CustomException.ResourceNotFoundException("User"));

        String accessToken = jwtUtils.generateAccessToken(user.getEmail());
        String refreshRaw = refreshService.createToken(user);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshRaw)
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshMs)).sameSite("Strict").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("accessToken", accessToken, "tokenType", "Bearer"));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException("Refresh token missing", 401);
        }

        RefreshToken rt = refreshService.findByRawToken(refreshToken)
                .orElseThrow(() -> new ApiException("Invalid refresh token", 401));

        if (refreshService.isExpired(rt)) {
            refreshService.delete(rt);
            throw new ApiException("Refresh token expired", 401);
        }

        User user = rt.getUser();
        refreshService.delete(rt);
        String newRaw = refreshService.createToken(user);
        String newAccess = jwtUtils.generateAccessToken(user.getEmail());

        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRaw)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshMs))
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("accessToken", newAccess, "tokenType", "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ApiException("Refresh token missing", 401);
        }

        refreshService.findByRawToken(refreshToken).ifPresent(refreshService::delete);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.noContent().build();
    }
}