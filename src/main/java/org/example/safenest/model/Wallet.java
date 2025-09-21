package org.example.safenest.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id","currency"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private BigDecimal reserved;
}