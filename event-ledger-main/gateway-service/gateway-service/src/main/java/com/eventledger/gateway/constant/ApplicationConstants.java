package com.eventledger.gateway.constant;

public final class ApplicationConstants {

    private ApplicationConstants() {
    }

    /* =========================
     * Trace Headers
     * ========================= */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    public static final String MDC_TRACE_ID = "traceId";

    /* =========================
     * HTTP Headers
     * ========================= */
    public static final String CONTENT_TYPE = "Content-Type";

    public static final String APPLICATION_JSON = "application/json";

    /* =========================
     * Account Service Endpoints
     * ========================= */
    public static final String ACCOUNT_TRANSACTION_API =
            "/accounts/%s/transactions";

}