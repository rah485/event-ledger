package com.eventledger.account.entity;

import com.eventledger.account.enums.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_tx_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_tx_account_id", columnList = "account_id"),
                @Index(name = "idx_tx_event_timestamp", columnList = "event_timestamp")
        }
)
public class TransactionEntity extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "account_id", nullable = false, length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;
}