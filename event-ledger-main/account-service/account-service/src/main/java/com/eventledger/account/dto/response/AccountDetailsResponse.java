package com.eventledger.account.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AccountDetailsResponse(

        String accountId,

        BigDecimal currentBalance,

        List<TransactionResponse> transactions

) {
}