package org.example.safenest.controller;

import org.example.safenest.model.DepositRequest;
import org.example.safenest.model.User;
import org.example.safenest.service.DepositService;
import org.example.safenest.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/deposit")
public class DepositController {

    private final DepositService depositService;
    private final UserService userService;

    public DepositController(DepositService depositService, UserService userService) {
        this.depositService = depositService;
        this.userService = userService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initDeposit(@AuthenticationPrincipal Object principal,
                                         @RequestParam BigDecimal amount,
                                         @RequestParam String currency) {

        User user = userService.getCurrentUser(principal);
        DepositRequest request = depositService.createDeposit(user, amount, currency);

        return ResponseEntity.ok(Map.of(
                "depositId", request.getId(),
                "status", request.getStatus(),
                "paymentUrl", "https://bank.example/pay?ref=" + request.getId()
        ));
    }

    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestParam String depositId,
                                            @RequestParam String status,
                                            @RequestParam String reference) {

        depositService.handleBankCallback(depositId, status, reference);
        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}