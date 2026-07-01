package com.eventledger.gateway.service;

import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.CreateEventResult;
import com.eventledger.gateway.dto.response.EventResponse;

import java.util.List;

public interface EventService {

    /**
     * Creates a new event.
     * If the eventId already exists, returns the existing event
     * without creating a duplicate (idempotency).
     *
     * @param request event request
     * @return created or existing event
     */
	 CreateEventResult createEvent(EventRequest request);

    /**
     * Retrieves an event by its business eventId.
     *
     * @param eventId business event identifier
     * @return event details
     */
	 EventResponse getEventByEventId(String eventId);

    /**
     * Retrieves all events for an account sorted by eventTimestamp.
     *
     * @param accountId account identifier
     * @return ordered list of events
     */
	 List<EventResponse> getEventsByAccountId(String accountId);

}