package com.eventledger.account.service.impl;

import com.eventledger.account.dto.request.AccountTransactionRequest;
import com.eventledger.account.dto.response.AccountDetailsResponse;
import com.eventledger.account.dto.response.AccountTransactionResponse;
import com.eventledger.account.entity.AccountEntity;
import com.eventledger.account.entity.TransactionEntity;
import com.eventledger.account.enums.TransactionType;
import com.eventledger.account.exception.ResourceNotFoundException;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private AccountTransactionRequest request;

    @BeforeEach
    void setUp() {
        request = new AccountTransactionRequest(
                "EVT-1001",
                "ACC-1001",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z")
        );
    }

    @Test
    void shouldApplyTransactionForNewAccount() {
        when(transactionRepository.findByEventId("EVT-1001")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountId("ACC-1001")).thenReturn(Optional.empty());
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountTransactionResponse response = accountService.applyTransaction("ACC-1001", request);

        assertEquals("ACC-1001", response.accountId());
        assertEquals(new BigDecimal("100.00"), response.currentBalance());
        assertEquals("Transaction processed successfully", response.message());

        verify(accountRepository).save(any(AccountEntity.class));
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    @Test
    void shouldIgnoreDuplicateTransactionEventId() {
        TransactionEntity existingTransaction = new TransactionEntity();
        existingTransaction.setEventId("EVT-1001");

        AccountEntity existingAccount = new AccountEntity();
        existingAccount.setAccountId("ACC-1001");
        existingAccount.setCurrentBalance(new BigDecimal("100.00"));

        when(transactionRepository.findByEventId("EVT-1001")).thenReturn(Optional.of(existingTransaction));
        when(accountRepository.findByAccountId("ACC-1001")).thenReturn(Optional.of(existingAccount));

        AccountTransactionResponse response = accountService.applyTransaction("ACC-1001", request);

        assertEquals(new BigDecimal("100.00"), response.currentBalance());
        assertEquals("Transaction already processed", response.message());

        verify(accountRepository, never()).save(any(AccountEntity.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void shouldReturnAccountDetailsWithTransactions() {
        AccountEntity account = new AccountEntity();
        account.setAccountId("ACC-1001");
        account.setCurrentBalance(new BigDecimal("75.00"));

        TransactionEntity credit = new TransactionEntity();
        credit.setEventId("EVT-1002");
        credit.setType(TransactionType.CREDIT);
        credit.setAmount(new BigDecimal("100.00"));
        credit.setCurrency("USD");
        credit.setEventTimestamp(Instant.parse("2026-01-01T11:00:00Z"));

        when(accountRepository.findByAccountId("ACC-1001")).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdOrderByEventTimestampDesc("ACC-1001"))
                .thenReturn(List.of(credit));

        AccountDetailsResponse response = accountService.getAccountDetails("ACC-1001");

        assertEquals("ACC-1001", response.accountId());
        assertEquals(new BigDecimal("75.00"), response.currentBalance());
        assertEquals(1, response.transactions().size());
        assertEquals("EVT-1002", response.transactions().get(0).eventId());
    }

    @Test
    void shouldThrowWhenAccountNotFoundForBalance() {
        when(accountRepository.findByAccountId("ACC-404")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getBalance("ACC-404"));

        assertTrue(exception.getMessage().contains("ACC-404"));
    }
}