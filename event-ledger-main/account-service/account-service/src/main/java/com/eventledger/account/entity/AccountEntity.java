package com.eventledger.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_account_id", columnList = "account_id", unique = true)
        }
)
public class AccountEntity extends BaseEntity {

    @Column(name = "account_id", nullable = false, unique = true, length = 100)
    private String accountId;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance = BigDecimal.ZERO;
}