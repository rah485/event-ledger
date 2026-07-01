package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.CreateEventResult;
import com.eventledger.gateway.dto.response.EventResponse;
import com.eventledger.gateway.enums.TransactionType;
import com.eventledger.gateway.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private EventService eventService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void shouldCreateNewEvent() throws Exception {

		EventRequest request = new EventRequest("EVT-1001", "ACC-1001", TransactionType.CREDIT, new BigDecimal("1000"),
				"INR", Instant.now(), null);

		EventResponse response = new EventResponse("EVT-1001", "ACC-1001", TransactionType.CREDIT,
				new BigDecimal("1000"), "INR", request.eventTimestamp(), null);

		CreateEventResult result = new CreateEventResult(response, true);

		when(eventService.createEvent(any(EventRequest.class))).thenReturn(result);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.eventId").value("EVT-1001")).andExpect(jsonPath("$.accountId").value("ACC-1001"))
				.andExpect(jsonPath("$.currency").value("INR"));
	}

	@Test
	void shouldReturnExistingEventForDuplicate() throws Exception {

		EventRequest request = new EventRequest("EVT-1001", "ACC-1001", TransactionType.CREDIT, new BigDecimal("1000"),
				"INR", Instant.now(), null);

		EventResponse response = new EventResponse("EVT-1001", "ACC-1001", TransactionType.CREDIT,
				new BigDecimal("1000"), "INR", request.eventTimestamp(), null);

		CreateEventResult result = new CreateEventResult(response, false);

		when(eventService.createEvent(any(EventRequest.class))).thenReturn(result);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value("EVT-1001"));
	}

	@Test
	void shouldGetEventByEventId() throws Exception {

		EventResponse response = new EventResponse("EVT-1001", "ACC-1001", TransactionType.CREDIT,
				new BigDecimal("1000"), "INR", Instant.now(), null);

		when(eventService.getEventByEventId("EVT-1001")).thenReturn(response);

		mockMvc.perform(get("/events/EVT-1001")).andExpect(status().isOk())
				.andExpect(jsonPath("$.eventId").value("EVT-1001"))
				.andExpect(jsonPath("$.accountId").value("ACC-1001"));
	}

	@Test
	void shouldGetEventsByAccountId() throws Exception {

		EventResponse response = new EventResponse("EVT-1001", "ACC-1001", TransactionType.CREDIT,
				new BigDecimal("1000"), "INR", Instant.now(), null);

		when(eventService.getEventsByAccountId("ACC-1001")).thenReturn(List.of(response));

		mockMvc.perform(get("/events").param("account", "ACC-1001")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].eventId").value("EVT-1001"))
				.andExpect(jsonPath("$[0].accountId").value("ACC-1001"));
	}

	@Test
	void shouldReturnBadRequestWhenValidationFails() throws Exception {

		EventRequest request = new EventRequest("", "", null, null, "", null, null);

		mockMvc.perform(post("/events").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());
	}

}