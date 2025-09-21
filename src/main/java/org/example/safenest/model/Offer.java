package org.example.safenest.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotBlank
    @Column(nullable = false)
    private String giveCurrency; // что продаём

    @NotNull
    @Column(nullable = false)
    private BigDecimal giveAmount; // сколько продаём

    @NotBlank
    @Column(nullable = false)
    private String wantCurrency; // что хотим взамен

    @NotNull
    @Column(nullable = false)
    private BigDecimal wantAmount; // сколько хотим взамен

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}