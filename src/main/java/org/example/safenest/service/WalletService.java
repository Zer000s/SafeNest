package org.example.safenest.service;

import org.example.safenest.exception.ApiException;
import org.example.safenest.model.User;
import org.example.safenest.model.Wallet;
import org.example.safenest.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class WalletService {
    private final WalletRepository repo;

    public WalletService(WalletRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public Wallet createOrGetWallet(User user, String currency) {
        return repo.findByUserAndCurrency(user, currency)
                .orElseGet(() -> repo.save(Wallet.builder()
                        .user(user)
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .reserved(BigDecimal.ZERO)
                        .build()));
    }

    @Transactional
    public void deposit(User user, String currency, BigDecimal amount) {
        if (amount.signum() <= 0){
            throw new ApiException("Deposit amount must be positive", 400);
        }
        Wallet w = createOrGetWallet(user, currency);
        w.setBalance(w.getBalance().add(amount));
        repo.save(w);
    }

    @Transactional
    public void reserve(User user, String currency, BigDecimal amount) {
        Wallet w = repo.findByUserAndCurrency(user, currency)
                .orElseThrow(() -> new ApiException("Wallet not found", 404));
        if (w.getBalance().compareTo(amount) < 0)
            throw new ApiException("Insufficient balance", 400);
        w.setBalance(w.getBalance().subtract(amount));
        w.setReserved(w.getReserved().add(amount));
        repo.save(w);
    }

    @Transactional
    public void releaseReserved(User user, String currency, BigDecimal amount) {
        Wallet w = repo.findByUserAndCurrency(user, currency)
                .orElseThrow(() -> new ApiException("Wallet not found", 404));
        if (w.getReserved().compareTo(amount) < 0)
            throw new ApiException("Not enough reserved balance", 400);
        w.setReserved(w.getReserved().subtract(amount));
        w.setBalance(w.getBalance().add(amount));
        repo.save(w);
    }

    @Transactional
    public void transferReserved(User fromUser, User toUser, String currency, BigDecimal amount) {
        Wallet from = repo.findByUserAndCurrency(fromUser, currency)
                .orElseThrow(() -> new ApiException("Sender wallet not found", 404));
        if (from.getReserved().compareTo(amount) < 0)
            throw new ApiException("Insufficient reserved funds", 400);

        Wallet to = createOrGetWallet(toUser, currency);

        from.setReserved(from.getReserved().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        repo.save(from);
        repo.save(to);
    }

    public BigDecimal getAvailableBalance(User user, String currency) {
        Wallet w = repo.findByUserAndCurrency(user, currency)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .reserved(BigDecimal.ZERO)
                        .build());
        return w.getBalance();
    }

    public BigDecimal getReservedBalance(User user, String currency) {
        Wallet w = repo.findByUserAndCurrency(user, currency)
                .orElseGet(() -> Wallet.builder()
                        .user(user)
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .reserved(BigDecimal.ZERO)
                        .build());
        return w.getReserved();
    }
}