package com.eventledger.gateway.dto.request;

import com.eventledger.gateway.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionRequest(

        @NotBlank
        String eventId,

        @NotBlank
        String accountId,

        @NotNull
        TransactionType type,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        Instant eventTimestamp

) {
}