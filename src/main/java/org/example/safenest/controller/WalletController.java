package org.example.safenest.controller;

import org.example.safenest.model.User;
import org.example.safenest.service.UserService;
import org.example.safenest.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/{currency}")
    public ResponseEntity<?> getBalance(@AuthenticationPrincipal Object principal,
                                        @PathVariable String currency) {

        User user = userService.getCurrentUser(principal);
        BigDecimal available = walletService.getAvailableBalance(user, currency);
        BigDecimal reserved = walletService.getReservedBalance(user, currency);

        return ResponseEntity.ok(Map.of(
                "currency", currency,
                "available", available,
                "reserved", reserved
        ));
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@AuthenticationPrincipal Object principal,
                                     @RequestParam String currency,
                                     @RequestParam BigDecimal amount) {

        User user = userService.getCurrentUser(principal);
        walletService.deposit(user, currency, amount);

        return ResponseEntity.ok(Map.of("message", "Deposit successful"));
    }
}