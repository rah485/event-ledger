package com.eventledger.account.dto.request;

import com.eventledger.account.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(

        @NotBlank(message = "Event ID is required")
        String eventId,

        @NotBlank(message = "Account ID is required")
        String accountId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotNull(message = "Event timestamp is required")
        Instant eventTimestamp

) {
}