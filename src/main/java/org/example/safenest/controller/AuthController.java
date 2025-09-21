package org.example.safenest.controller;

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
    public ResponseEntity<?> register(@RequestBody Map<String,String> body) {
        String email = body.get("email");
        String pwd = body.get("password");
        if(email == null || pwd == null) {
            return ResponseEntity.badRequest().build();
        }
        if(userRepo.existsByEmail(email)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email exists");
        }
        User u = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(pwd))
                .createdAt(Instant.now())
                .kycVerified(false)
                .build();
        userRepo.save(u);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body, HttpServletResponse response) {
        String email = body.get("email");
        String pwd = body.get("password");
        Authentication a = authManager.authenticate(new UsernamePasswordAuthenticationToken(email, pwd));
        UserPrincipal up = (UserPrincipal) a.getPrincipal();
        String access = jwtUtils.generateAccessToken(up.getUsername());
        String refreshRaw = refreshService.createToken(userRepo.findByEmail(up.getUsername()).orElseThrow());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshRaw)
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshMs)).sameSite("Strict").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("accessToken", access, "tokenType", "Bearer"));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                     HttpServletResponse response) {
        if (refreshToken == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var opt = refreshService.findByRawToken(refreshToken);
        if (opt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        var rt = opt.get();
        if (refreshService.isExpired(rt)) {
            refreshService.delete(rt);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = rt.getUser();
        refreshService.delete(rt);
        String newRaw = refreshService.createToken(user);

        String newAccess = jwtUtils.generateAccessToken(user.getEmail());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRaw)
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(refreshMs)).sameSite("Strict").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Map.of("accessToken", newAccess, "tokenType", "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader, HttpServletResponse response) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return ResponseEntity.noContent().build();
        String token = authHeader.substring(7);
        String username = jwtUtils.getUsername(token);
        userRepo.findByEmail(username).ifPresent(refreshService::deleteAllForUser);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).path("/api/auth/refresh")
                .maxAge(0).sameSite("Strict").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
    }
}