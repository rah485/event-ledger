package com.eventledger.account.dto.response;

import com.eventledger.account.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(

        String eventId,

        TransactionType type,

        BigDecimal amount,

        String currency,

        Instant eventTimestamp

) {
}