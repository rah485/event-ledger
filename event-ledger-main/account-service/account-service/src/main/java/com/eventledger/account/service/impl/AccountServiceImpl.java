package com.eventledger.account.service.impl;

import com.eventledger.account.dto.request.AccountTransactionRequest;
import com.eventledger.account.dto.response.AccountBalanceResponse;
import com.eventledger.account.dto.response.AccountDetailsResponse;
import com.eventledger.account.dto.response.AccountTransactionResponse;
import com.eventledger.account.dto.response.TransactionResponse;
import com.eventledger.account.entity.AccountEntity;
import com.eventledger.account.entity.TransactionEntity;
import com.eventledger.account.enums.TransactionType;
import com.eventledger.account.exception.ResourceNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.eventledger.account.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public AccountTransactionResponse applyTransaction(String accountId,
                                                       AccountTransactionRequest request) {

        validateAccountId(accountId, request.accountId());

        log.info("Applying transaction. accountId={} eventId={} type={} amount={}",
                accountId, request.eventId(), request.type(), request.amount());

        TransactionEntity existingTransaction = transactionRepository
                .findByEventId(request.eventId())
                .orElse(null);

        if (existingTransaction != null) {
            log.info("Duplicate transaction detected. accountId={} eventId={}",
                    accountId, request.eventId());

            AccountEntity existingAccount = findAccount(accountId);

            return new AccountTransactionResponse(
                    existingAccount.getAccountId(),
                    existingAccount.getCurrentBalance(),
                    "Transaction already processed",
                    Instant.now()
            );
        }

        AccountEntity account = accountRepository.findByAccountId(accountId)
                .orElseGet(() -> buildAccount(accountId));

        account.setCurrentBalance(applyBalanceChange(
                account.getCurrentBalance(),
                request.type(),
                request.amount()));

        AccountEntity savedAccount = accountRepository.save(account);
        transactionRepository.save(buildTransaction(request));

        log.info("Transaction applied successfully. accountId={} balance={}",
                savedAccount.getAccountId(), savedAccount.getCurrentBalance());

        return new AccountTransactionResponse(
                savedAccount.getAccountId(),
                savedAccount.getCurrentBalance(),
                "Transaction processed successfully",
                Instant.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(String accountId) {

        log.info("Fetching account balance. accountId={}", accountId);

        AccountEntity account = findAccount(accountId);

        return new AccountBalanceResponse(
                account.getAccountId(),
                account.getCurrentBalance()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailsResponse getAccountDetails(String accountId) {

        log.info("Fetching account details. accountId={}", accountId);

        AccountEntity account = findAccount(accountId);

        List<TransactionResponse> transactions = transactionRepository
                .findByAccountIdOrderByEventTimestampDesc(accountId)
                .stream()
                .map(this::toTransactionResponse)
                .toList();

        return new AccountDetailsResponse(
                account.getAccountId(),
                account.getCurrentBalance(),
                transactions
        );
    }

    private void validateAccountId(String pathAccountId, String bodyAccountId) {
        if (!pathAccountId.equals(bodyAccountId)) {
            throw new IllegalArgumentException("Path accountId must match request accountId");
        }
    }

    private AccountEntity findAccount(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with accountId: " + accountId));
    }

    private AccountEntity buildAccount(String accountId) {
        AccountEntity account = new AccountEntity();
        account.setAccountId(accountId);
        account.setCurrentBalance(BigDecimal.ZERO);
        return account;
    }

    private TransactionEntity buildTransaction(AccountTransactionRequest request) {
        TransactionEntity transaction = new TransactionEntity();
        transaction.setEventId(request.eventId());
        transaction.setAccountId(request.accountId());
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setEventTimestamp(request.eventTimestamp());
        return transaction;
    }

    private BigDecimal applyBalanceChange(BigDecimal currentBalance,
                                          TransactionType type,
                                          BigDecimal amount) {
        return switch (type) {
            case CREDIT -> currentBalance.add(amount);
            case DEBIT -> currentBalance.subtract(amount);
        };
    }

    private TransactionResponse toTransactionResponse(TransactionEntity transaction) {
        return new TransactionResponse(
                transaction.getEventId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getEventTimestamp()
        );
    }
}