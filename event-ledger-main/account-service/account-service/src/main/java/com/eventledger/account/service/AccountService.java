package com.eventledger.account.service;

import com.eventledger.account.dto.request.AccountTransactionRequest;
import com.eventledger.account.dto.response.AccountBalanceResponse;
import com.eventledger.account.dto.response.AccountDetailsResponse;
import com.eventledger.account.dto.response.AccountTransactionResponse;

public interface AccountService {

    AccountTransactionResponse applyTransaction(String accountId,
                                               AccountTransactionRequest request);

    AccountBalanceResponse getBalance(String accountId);

    AccountDetailsResponse getAccountDetails(String accountId);
}