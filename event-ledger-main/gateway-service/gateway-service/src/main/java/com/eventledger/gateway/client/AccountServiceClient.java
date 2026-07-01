package com.eventledger.gateway.client;

import com.eventledger.gateway.config.AccountServiceProperties;
import com.eventledger.gateway.constant.ApplicationConstants;
import com.eventledger.gateway.dto.request.AccountTransactionRequest;
import com.eventledger.gateway.dto.response.AccountTransactionResponse;
import com.eventledger.gateway.exception.ServiceUnavailableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

@Component
public class AccountServiceClient {

    private static final Logger log =
            LoggerFactory.getLogger(AccountServiceClient.class);

    private static final Duration ACCOUNT_SERVICE_TIMEOUT = Duration.ofSeconds(3);
    private static final String UNKNOWN_TRACE_ID = "UNKNOWN";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AccountServiceProperties properties;

    public AccountServiceClient(HttpClient httpClient,
                                AccountServiceProperties properties,
                                ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Retry(name = "accountService")
    public AccountTransactionResponse applyTransaction(
            AccountTransactionRequest request) {

        String url = buildTransactionUrl(request.accountId());
        String traceId = resolveTraceId();

        log.info("Calling Account Service. url={} accountId={} traceId={}",
                url, request.accountId(), traceId);

        try {

            HttpResponse<String> response = httpClient.send(
                    buildHttpRequest(request, url, traceId),
                    HttpResponse.BodyHandlers.ofString());

            AccountTransactionResponse result = mapResponse(response, request.accountId());

            log.info("Account Service call succeeded. accountId={} balance={}",
                    result.accountId(), result.currentBalance());

            return result;

        } catch (ConnectException ex) {

            log.error("Cannot connect to Account Service. url={} error={}",
                    url, ex.getMessage());

            throw new ServiceUnavailableException(
                    "Unable to connect to Account Service.", ex);

        } catch (JsonProcessingException ex) {

            log.error("Payload serialization error for Account Service. accountId={} error={}",
                    request.accountId(), ex.getMessage());

            throw new ServiceUnavailableException(
                    "Failed to serialize/deserialize Account Service payload.", ex);

        } catch (IOException ex) {

            log.error("I/O error communicating with Account Service. url={} error={}",
                    url, ex.getMessage());

            throw new ServiceUnavailableException(
                    "Communication error while calling Account Service.", ex);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            log.error("Thread interrupted while calling Account Service. url={}", url);

            throw new ServiceUnavailableException(
                    "Thread interrupted while calling Account Service.", ex);
        }
    }

    private HttpRequest buildHttpRequest(AccountTransactionRequest request,
                                         String url,
                                         String traceId)
            throws JsonProcessingException {

        Objects.requireNonNull(request, "Account transaction request is required.");

        String requestBody = objectMapper.writeValueAsString(request);

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(ACCOUNT_SERVICE_TIMEOUT)
                .header(ApplicationConstants.CONTENT_TYPE, ApplicationConstants.APPLICATION_JSON)
                .header(ApplicationConstants.TRACE_ID_HEADER, traceId)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private String buildTransactionUrl(String accountId) {
        return properties.baseUrl()
                + String.format(ApplicationConstants.ACCOUNT_TRANSACTION_API, accountId);
    }

    private String resolveTraceId() {
        String traceId = MDC.get(ApplicationConstants.MDC_TRACE_ID);
        return (traceId == null || traceId.isBlank()) ? UNKNOWN_TRACE_ID : traceId;
    }

    private AccountTransactionResponse mapResponse(HttpResponse<String> response,
                                                    String accountId)
            throws JsonProcessingException {

        int status = response.statusCode();

        if (status < 200 || status >= 300) {

            log.error("Account Service returned error status. accountId={} httpStatus={}",
                    accountId, status);

            throw new ServiceUnavailableException(
                    "Account Service returned HTTP " + status);
        }

        return objectMapper.readValue(response.body(), AccountTransactionResponse.class);
    }
}
