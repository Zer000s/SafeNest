package org.example.safenest.repository;

import org.example.safenest.model.User;
import org.example.safenest.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUserAndCurrency(User user, String currency);
}