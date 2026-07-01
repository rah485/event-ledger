package com.eventledger.gateway.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountTransactionResponse(

        String accountId,

        BigDecimal currentBalance,

        String message,

        Instant processedAt

) {
}