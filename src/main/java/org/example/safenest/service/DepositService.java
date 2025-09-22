package org.example.safenest.service;

import jakarta.transaction.Transactional;
import org.example.safenest.exception.ApiException;
import org.example.safenest.model.DepositRequest;
import org.example.safenest.model.DepositStatus;
import org.example.safenest.model.User;
import org.example.safenest.repository.DepositRequestRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DepositService {

    private final DepositRequestRepository depositRepo;
    private final WalletService walletService;

    public DepositService(DepositRequestRepository depositRepo, WalletService walletService) {
        this.depositRepo = depositRepo;
        this.walletService = walletService;
    }

    public DepositRequest createDeposit(User user, BigDecimal amount, String currency) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException("Deposit amount must be positive", 400);
        }

        DepositRequest request = DepositRequest.builder()
                .user(user)
                .amount(amount)
                .currency(currency)
                .status(DepositStatus.PENDING)
                .build();

        return depositRepo.save(request);
    }

    @Transactional
    public void handleBankCallback(String depositId, String status, String reference) {
        DepositRequest request = depositRepo.findById(depositId)
                .orElseThrow(() -> new ApiException("Deposit request not found", 404));

        if (request.getStatus() != DepositStatus.PENDING) {
            throw new ApiException("Deposit already processed", 400);
        }

        request.setPaymentReference(reference);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            request.setStatus(DepositStatus.COMPLETED);
            walletService.deposit(request.getUser(), request.getCurrency(), request.getAmount());
        } else {
            request.setStatus(DepositStatus.FAILED);
        }

        depositRepo.save(request);
    }
}