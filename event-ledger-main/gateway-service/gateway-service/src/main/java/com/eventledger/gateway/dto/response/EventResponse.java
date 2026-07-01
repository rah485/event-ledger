package com.eventledger.gateway.dto.response;

import com.eventledger.gateway.dto.request.MetadataRequest;
import com.eventledger.gateway.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(

        String eventId,

        String accountId,

        TransactionType type,

        BigDecimal amount,

        String currency,

        Instant eventTimestamp,

        MetadataRequest metadata

) {
}