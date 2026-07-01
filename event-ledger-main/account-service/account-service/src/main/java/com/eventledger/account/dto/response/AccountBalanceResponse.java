package com.eventledger.account.dto.response;

import java.math.BigDecimal;

public record AccountBalanceResponse(

        String accountId,

        BigDecimal currentBalance

) {
}