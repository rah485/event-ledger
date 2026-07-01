package com.eventledger.gateway.service.impl;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.CreateEventResult;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.enums.TransactionType;
import com.eventledger.gateway.mapper.EventMapper;
import com.eventledger.gateway.metrics.MetricsService;
import com.eventledger.gateway.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private EventServiceImpl eventService;

    private EventRequest eventRequest;

    private EventEntity eventEntity;

    @BeforeEach
    void setUp() {

        eventRequest = new EventRequest(
                "EVT-1001",
                "ACC-1001",
                TransactionType.CREDIT,
                new BigDecimal("1000"),
                "INR",
                Instant.now(),
                null
        );

        eventEntity = new EventEntity();

        eventEntity.setEventId("EVT-1001");
        eventEntity.setAccountId("ACC-1001");
        eventEntity.setType(TransactionType.CREDIT);
        eventEntity.setAmount(new BigDecimal("1000"));
        eventEntity.setCurrency("INR");
        eventEntity.setEventTimestamp(eventRequest.eventTimestamp());

    }

    @Test
    void shouldCreateEventSuccessfully() {

        when(eventRepository.findByEventId("EVT-1001"))
                .thenReturn(Optional.empty());

        when(eventMapper.toEntity(eventRequest))
                .thenReturn(eventEntity);

        when(eventRepository.save(eventEntity))
                .thenReturn(eventEntity);

        when(eventMapper.toResponse(eventEntity))
                .thenReturn(new com.eventledger.gateway.dto.response.EventResponse(
                        eventEntity.getEventId(),
                        eventEntity.getAccountId(),
                        eventEntity.getType(),
                        eventEntity.getAmount(),
                        eventEntity.getCurrency(),
                        eventEntity.getEventTimestamp(),
                        null
                ));

        CreateEventResult result =
                eventService.createEvent(eventRequest);

        assertNotNull(result);

        assertTrue(result.created());

        verify(eventRepository).save(eventEntity);

        verify(accountServiceClient)
                .applyTransaction(ArgumentMatchers.any());

        verify(metricsService)
                .incrementEventCounter();
    }

    @Test
    void shouldReturnExistingEventWhenDuplicateEventId() {

        when(eventRepository.findByEventId("EVT-1001"))
                .thenReturn(Optional.of(eventEntity));

        when(eventMapper.toResponse(eventEntity))
                .thenReturn(new com.eventledger.gateway.dto.response.EventResponse(
                        eventEntity.getEventId(),
                        eventEntity.getAccountId(),
                        eventEntity.getType(),
                        eventEntity.getAmount(),
                        eventEntity.getCurrency(),
                        eventEntity.getEventTimestamp(),
                        null
                ));

        CreateEventResult result =
                eventService.createEvent(eventRequest);

        assertFalse(result.created());

        verify(eventRepository, never())
                .save(any());

        verify(accountServiceClient, never())
                .applyTransaction(any());

        verify(metricsService, never())
                .incrementEventCounter();
    }

}