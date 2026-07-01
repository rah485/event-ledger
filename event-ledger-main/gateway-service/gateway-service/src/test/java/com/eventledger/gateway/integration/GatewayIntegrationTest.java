package com.eventledger.gateway.integration;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.constant.ApplicationConstants;
import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.AccountTransactionResponse;
import com.eventledger.gateway.enums.TransactionType;
import com.eventledger.gateway.exception.ServiceUnavailableException;
import com.eventledger.gateway.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GatewayIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private EventRepository eventRepository;

	@MockBean
	private AccountServiceClient accountServiceClient;

	@BeforeEach
	void setup() {

		eventRepository.deleteAll();

		when(accountServiceClient.applyTransaction(any())).thenReturn(new AccountTransactionResponse("ACC-1001",
				new BigDecimal("5000.00"), "Transaction processed successfully", Instant.now()));
	}

	@Test
	void shouldCreateEventSuccessfully() throws Exception {

		EventRequest request = new EventRequest("EVT-1001", "ACC-1001", TransactionType.CREDIT, new BigDecimal("1000"),
				"INR", Instant.now(), null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))

				.andExpect(status().isCreated()).andExpect(jsonPath("$.eventId").value("EVT-1001"))
				.andExpect(jsonPath("$.accountId").value("ACC-1001"));
	}

	@Test
	void shouldReturnExistingEventWhenDuplicateEventId() throws Exception {

		EventRequest request = new EventRequest("EVT-1002", "ACC-1001", TransactionType.CREDIT, new BigDecimal("1000"),
				"INR", Instant.now(), null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
	}

	@Test
	void shouldGetEventByEventId() throws Exception {

		EventRequest request = new EventRequest("EVT-1003", "ACC-1002", TransactionType.DEBIT, new BigDecimal("250"),
				"INR", Instant.now(), null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated());

		mockMvc.perform(get("/events/EVT-1003")).andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value("EVT-1003"))
				.andExpect(jsonPath("$.accountId").value("ACC-1002"));
	}

	@Test
	void shouldGetEventsByAccountId() throws Exception {

		EventRequest request1 = new EventRequest("EVT-2001", "ACC-9999", TransactionType.CREDIT, new BigDecimal("100"),
				"INR", Instant.now(), null);

		EventRequest request2 = new EventRequest("EVT-2002", "ACC-9999", TransactionType.DEBIT, new BigDecimal("50"),
				"INR", Instant.now().plusSeconds(60), null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request1)));

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request2)));

		mockMvc.perform(get("/events").param("account", "ACC-9999")).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));
	}

	@Test
	void shouldReturnBadRequestWhenValidationFails() throws Exception {

		EventRequest request = new EventRequest("", "", null, null, "", null, null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

	/**
	 * REQ: Graceful degradation — when Account Service is unavailable,
	 * POST /events must return 503 Service Unavailable.
	 */
	@Test
	void shouldReturn503WhenAccountServiceIsUnavailable() throws Exception {

		when(accountServiceClient.applyTransaction(any()))
				.thenThrow(new ServiceUnavailableException("Account Service is unavailable"));

		EventRequest request = new EventRequest("EVT-3001", "ACC-3001", TransactionType.CREDIT,
				new BigDecimal("500"), "USD", Instant.now(), null);

		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.error").value("Service Unavailable"));
	}

	/**
	 * REQ: Graceful degradation — GET /events reads must still work
	 * even when Account Service is unavailable.
	 */
	@Test
	void shouldStillServeGetEventsWhenAccountServiceIsDown() throws Exception {

		// Create an event while account service is up
		EventRequest request = new EventRequest("EVT-3002", "ACC-3002", TransactionType.CREDIT,
				new BigDecimal("200"), "USD", Instant.now(), null);

		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated());

		// Account service goes down — GET reads must not be affected
		when(accountServiceClient.applyTransaction(any()))
				.thenThrow(new ServiceUnavailableException("Account Service is unavailable"));

		mockMvc.perform(get("/events/EVT-3002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value("EVT-3002"));

		mockMvc.perform(get("/events").param("account", "ACC-3002"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	/**
	 * REQ: Out-of-order tolerance — events submitted with earlier timestamps
	 * must be returned sorted by eventTimestamp ascending.
	 */
	@Test
	void shouldReturnEventsOrderedByEventTimestampRegardlessOfArrivalOrder() throws Exception {

		Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
		Instant t2 = Instant.parse("2026-01-01T11:00:00Z");
		Instant t3 = Instant.parse("2026-01-01T12:00:00Z");

		// Submit out of order: t3 first, then t1, then t2
		EventRequest late = new EventRequest("EVT-OOO-3", "ACC-OOO", TransactionType.CREDIT,
				new BigDecimal("300"), "USD", t3, null);
		EventRequest early = new EventRequest("EVT-OOO-1", "ACC-OOO", TransactionType.CREDIT,
				new BigDecimal("100"), "USD", t1, null);
		EventRequest middle = new EventRequest("EVT-OOO-2", "ACC-OOO", TransactionType.DEBIT,
				new BigDecimal("200"), "USD", t2, null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(late)));
		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(early)));
		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(middle)));

		// Must be returned sorted by eventTimestamp ASC regardless of arrival order
		mockMvc.perform(get("/events").param("account", "ACC-OOO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].eventId").value("EVT-OOO-1"))
				.andExpect(jsonPath("$[1].eventId").value("EVT-OOO-2"))
				.andExpect(jsonPath("$[2].eventId").value("EVT-OOO-3"));
	}

	/**
	 * REQ: Trace propagation — the X-Trace-Id header supplied by the client
	 * must be echoed back in the response, confirming the TraceIdFilter
	 * picked it up and stored it in MDC for the full request lifecycle.
	 */
	@Test
	void shouldPropagateTraceIdFromRequestToResponse() throws Exception {

		String knownTraceId = "test-trace-id-abc-123";

		EventRequest request = new EventRequest("EVT-4001", "ACC-4001", TransactionType.CREDIT,
				new BigDecimal("100"), "USD", Instant.now(), null);

		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.header(ApplicationConstants.TRACE_ID_HEADER, knownTraceId)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().string(ApplicationConstants.TRACE_ID_HEADER, is(knownTraceId)));
	}

	/**
	 * REQ: Trace propagation — when no X-Trace-Id is supplied, the Gateway
	 * must generate one and return it in the response header.
	 */
	@Test
	void shouldGenerateTraceIdWhenNotSuppliedByClient() throws Exception {

		EventRequest request = new EventRequest("EVT-4002", "ACC-4002", TransactionType.CREDIT,
				new BigDecimal("100"), "USD", Instant.now(), null);

		mockMvc.perform(post("/events")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(header().exists(ApplicationConstants.TRACE_ID_HEADER));
	}
}