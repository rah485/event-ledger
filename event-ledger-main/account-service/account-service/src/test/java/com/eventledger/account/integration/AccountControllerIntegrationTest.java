package com.eventledger.account.integration;

import com.eventledger.account.constant.ApplicationConstants;
import com.eventledger.account.dto.request.AccountTransactionRequest;
import com.eventledger.account.enums.TransactionType;
import com.eventledger.account.repository.AccountRepository;
import com.eventledger.account.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldApplyTransactionAndReturnBalance() throws Exception {
        AccountTransactionRequest request = new AccountTransactionRequest(
                "EVT-2001",
                "ACC-2001",
                TransactionType.CREDIT,
                new BigDecimal("250.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z")
        );

        mockMvc.perform(post("/accounts/ACC-2001/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-2001"))
                .andExpect(jsonPath("$.currentBalance").value(250.00));

        mockMvc.perform(get("/accounts/ACC-2001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("ACC-2001"))
                .andExpect(jsonPath("$.currentBalance").value(250.00));
    }

    @Test
    void shouldIgnoreDuplicateEventId() throws Exception {
        AccountTransactionRequest request = new AccountTransactionRequest(
                "EVT-2002",
                "ACC-2002",
                TransactionType.CREDIT,
                new BigDecimal("100.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z")
        );

        mockMvc.perform(post("/accounts/ACC-2002/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/ACC-2002/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction already processed"));

        mockMvc.perform(get("/accounts/ACC-2002/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(100.00));
    }

    @Test
    void shouldKeepBalanceCorrectForOutOfOrderTransactions() throws Exception {
        AccountTransactionRequest later = new AccountTransactionRequest(
                "EVT-2003",
                "ACC-2003",
                TransactionType.CREDIT,
                new BigDecimal("300.00"),
                "USD",
                Instant.parse("2026-01-01T12:00:00Z")
        );

        AccountTransactionRequest earlier = new AccountTransactionRequest(
                "EVT-2004",
                "ACC-2003",
                TransactionType.DEBIT,
                new BigDecimal("125.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z")
        );

        mockMvc.perform(post("/accounts/ACC-2003/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(later)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/accounts/ACC-2003/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlier)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/ACC-2003/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentBalance").value(175.00));
    }

    @Test
    void shouldReturnBadRequestForInvalidTransaction() throws Exception {
        String invalidRequest = """
                {
                  \"eventId\": \"\",
                  \"accountId\": \"ACC-2004\",
                  \"type\": null,
                  \"amount\": 0,
                  \"currency\": \"\",
                  \"eventTimestamp\": null
                }
                """;

        mockMvc.perform(post("/accounts/ACC-2004/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldEchoTraceIdHeader() throws Exception {
        AccountTransactionRequest request = new AccountTransactionRequest(
                "EVT-2005",
                "ACC-2005",
                TransactionType.CREDIT,
                new BigDecimal("50.00"),
                "USD",
                Instant.parse("2026-01-01T10:00:00Z")
        );

        mockMvc.perform(post("/accounts/ACC-2005/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ApplicationConstants.TRACE_ID_HEADER, "trace-acc-123")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(ApplicationConstants.TRACE_ID_HEADER, is("trace-acc-123")));
    }
}