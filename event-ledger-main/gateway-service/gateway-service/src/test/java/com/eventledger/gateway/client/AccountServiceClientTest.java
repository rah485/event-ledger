package com.eventledger.gateway.client;

import com.eventledger.gateway.config.AccountServiceProperties;
import com.eventledger.gateway.constant.ApplicationConstants;
import com.eventledger.gateway.dto.request.AccountTransactionRequest;
import com.eventledger.gateway.dto.response.AccountTransactionResponse;
import com.eventledger.gateway.enums.TransactionType;
import com.eventledger.gateway.exception.ServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AccountServiceProperties properties;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private AccountServiceClient accountServiceClient;

    private AccountTransactionRequest request;

    @BeforeEach
    void setUp() {

        request = new AccountTransactionRequest(
                "EVT-1001",
                "ACC-1001",
                TransactionType.CREDIT,
                new BigDecimal("1000.00"),
                "INR",
                Instant.now()
        );
    }

    @Test
    void shouldApplyTransactionSuccessfully() throws Exception {

        AccountTransactionResponse expectedResponse =
                new AccountTransactionResponse(
                        "ACC-1001",
                        new BigDecimal("5000.00"),
                        "Transaction processed successfully.",
                        Instant.now()
                );

        when(properties.baseUrl())
                .thenReturn("http://localhost:8081");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        when(httpResponse.statusCode())
                .thenReturn(200);

        when(httpResponse.body())
                .thenReturn("{\"response\":\"ok\"}");

        when(objectMapper.readValue(
                anyString(),
                eq(AccountTransactionResponse.class)))
                .thenReturn(expectedResponse);

        AccountTransactionResponse response =
                accountServiceClient.applyTransaction(request);

        assertNotNull(response);
        assertEquals("ACC-1001", response.accountId());
        assertEquals(
                new BigDecimal("5000.00"),
                response.currentBalance());

        verify(httpClient)
                .send(any(HttpRequest.class),
                        any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldThrowServiceUnavailableWhenHttpStatusIs500() throws Exception {

        when(properties.baseUrl())
                .thenReturn("http://localhost:8081");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        when(httpResponse.statusCode())
                .thenReturn(500);

        assertThrows(
                ServiceUnavailableException.class,
                () -> accountServiceClient.applyTransaction(request));
    }

    @Test
    void shouldThrowServiceUnavailableWhenIOExceptionOccurs() throws Exception {

        when(properties.baseUrl())
                .thenReturn("http://localhost:8081");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(
                ServiceUnavailableException.class,
                () -> accountServiceClient.applyTransaction(request));
    }

    @Test
    void shouldThrowServiceUnavailableWhenInterrupted() throws Exception {

        when(properties.baseUrl())
                .thenReturn("http://localhost:8081");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Interrupted"));

        assertThrows(
                ServiceUnavailableException.class,
                () -> accountServiceClient.applyTransaction(request));

        assertTrue(Thread.currentThread().isInterrupted());

        // Clear interrupt flag so it doesn't affect other tests
        Thread.interrupted();
    }

    @Test
    void shouldPropagateTraceIdHeaderToAccountService() throws Exception {

        String traceId = "trace-propagation-test-123";
        MDC.put(ApplicationConstants.MDC_TRACE_ID, traceId);

        AccountTransactionResponse expectedResponse =
                new AccountTransactionResponse(
                        "ACC-1001",
                        new BigDecimal("5000.00"),
                        "Transaction processed successfully.",
                        Instant.now()
                );

        when(properties.baseUrl())
                .thenReturn("http://localhost:8081");

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"dummy\":\"json\"}");

        when(httpClient.send(
                any(HttpRequest.class),
                any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        when(httpResponse.statusCode())
                .thenReturn(200);

        when(httpResponse.body())
                .thenReturn("{\"response\":\"ok\"}");

        when(objectMapper.readValue(
                anyString(),
                eq(AccountTransactionResponse.class)))
                .thenReturn(expectedResponse);

        accountServiceClient.applyTransaction(request);

        verify(httpClient).send(argThat(httpRequest -> {
                    Optional<String> header = httpRequest.headers()
                            .firstValue(ApplicationConstants.TRACE_ID_HEADER);
                    return header.isPresent() && traceId.equals(header.get());
                }), any(HttpResponse.BodyHandler.class));

        MDC.remove(ApplicationConstants.MDC_TRACE_ID);
    }
}