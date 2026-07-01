package com.eventledger.account.controller;

import com.eventledger.account.dto.request.AccountTransactionRequest;
import com.eventledger.account.dto.response.AccountBalanceResponse;
import com.eventledger.account.dto.response.AccountDetailsResponse;
import com.eventledger.account.dto.response.AccountTransactionResponse;
import com.eventledger.account.service.AccountService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private static final Logger log =
            LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/transactions")
    public ResponseEntity<AccountTransactionResponse> applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody AccountTransactionRequest request) {

        log.info("Received transaction request. accountId={} eventId={}",
                accountId, request.eventId());

        AccountTransactionResponse response =
                accountService.applyTransaction(accountId, request);

        log.info("Transaction request completed. accountId={} balance={}",
                response.accountId(), response.currentBalance());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable String accountId) {

        log.info("Fetching balance. accountId={}", accountId);

        return ResponseEntity.ok(accountService.getBalance(accountId));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountDetailsResponse> getAccountDetails(
            @PathVariable String accountId) {

        log.info("Fetching account details. accountId={}", accountId);

        return ResponseEntity.ok(accountService.getAccountDetails(accountId));
    }
}