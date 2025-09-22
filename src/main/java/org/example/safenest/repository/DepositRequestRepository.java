package org.example.safenest.repository;

import org.example.safenest.model.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositRequestRepository extends JpaRepository<DepositRequest, String> {
}