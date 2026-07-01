package com.eventledger.gateway.dto.request;

import com.eventledger.gateway.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record EventRequest(

        @NotBlank(message = "Event ID is required")
        @Size(max = 100, message = "Event ID cannot exceed 100 characters")
        String eventId,

        @NotBlank(message = "Account ID is required")
        @Size(max = 100, message = "Account ID cannot exceed 100 characters")
        String accountId,

        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true,
                message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(max = 10, message = "Currency cannot exceed 10 characters")
        String currency,

        @NotNull(message = "Event timestamp is required")
        Instant eventTimestamp,

        @Valid
        MetadataRequest metadata

) {
}