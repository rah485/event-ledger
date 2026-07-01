package com.eventledger.gateway.service.impl;

import com.eventledger.gateway.client.AccountServiceClient;
import com.eventledger.gateway.metrics.MetricsService;
import com.eventledger.gateway.dto.request.AccountTransactionRequest;
import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.CreateEventResult;
import com.eventledger.gateway.dto.response.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import com.eventledger.gateway.exception.ResourceNotFoundException;
import com.eventledger.gateway.mapper.EventMapper;
import com.eventledger.gateway.repository.EventRepository;
import com.eventledger.gateway.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final EventMapper eventMapper;

    private final AccountServiceClient accountServiceClient;
    private final MetricsService metricsService;

    public EventServiceImpl(EventRepository eventRepository,
            EventMapper eventMapper,
            AccountServiceClient accountServiceClient,
            MetricsService metricsService) {

        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
        this.accountServiceClient = accountServiceClient;
        this.metricsService = metricsService;
    }

    /**
     * Creates a new event.
     * Implements idempotency based on eventId.
     */
    @Override
        public CreateEventResult createEvent(EventRequest request) {

        log.info("Received event with eventId={}", request.eventId());

        EventEntity existingEvent = eventRepository
                .findByEventId(request.eventId())
                .orElse(null);

        if (existingEvent != null) {

            log.info("Duplicate event detected. eventId={}", request.eventId());

            return new CreateEventResult(
                    eventMapper.toResponse(existingEvent),
                    false
            );
        }

        EventEntity eventEntity = eventMapper.toEntity(request);
        EventEntity savedEvent = eventRepository.save(eventEntity);

        metricsService.incrementEventCounter();

        log.info("Event saved successfully. eventId={}",
                savedEvent.getEventId());

        accountServiceClient.applyTransaction(
                buildAccountTransactionRequest(savedEvent));

        log.info("Transaction successfully applied to Account Service. accountId={}",
                savedEvent.getAccountId());

        return new CreateEventResult(
                eventMapper.toResponse(savedEvent),
                true
        );
    }

    /**
     * Retrieve an event using its business eventId.
     */
    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventByEventId(String eventId) {

        log.info("Fetching event with eventId={}", eventId);

        EventEntity eventEntity = eventRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event not found with eventId: " + eventId));

        log.info("Event found. eventId={}", eventId);

        return eventMapper.toResponse(eventEntity);
    }

    /**
     * Retrieve all events for an account ordered by eventTimestamp.
     */
    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByAccountId(String accountId) {

        log.info("Fetching events for accountId={}", accountId);

        List<EventEntity> events = eventRepository
                .findByAccountIdOrderByEventTimestampAsc(accountId);

        log.info("Found {} events for accountId={}",
                events.size(),
                accountId);

        return events.stream()
                .map(eventMapper::toResponse)
                .toList();
    }

    private AccountTransactionRequest buildAccountTransactionRequest(
            EventEntity eventEntity) {

        return new AccountTransactionRequest(
                eventEntity.getEventId(),
                eventEntity.getAccountId(),
                eventEntity.getType(),
                eventEntity.getAmount(),
                eventEntity.getCurrency(),
                eventEntity.getEventTimestamp()
        );
    }
}



