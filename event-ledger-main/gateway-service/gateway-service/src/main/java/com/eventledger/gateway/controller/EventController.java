package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.response.CreateEventResult;
import com.eventledger.gateway.dto.response.EventResponse;
import com.eventledger.gateway.service.EventService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private static final Logger log =
            LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Creates a new financial event.
     * Returns:
     * 201 Created -> New event
     * 200 OK -> Duplicate event (Idempotency)
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody EventRequest request) {

        log.info("Received create event request. eventId={}",
                request.eventId());

        CreateEventResult result = eventService.createEvent(request);

        if (result.created()) {

            log.info("Event created successfully. eventId={}",
                    result.event().eventId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(result.event());
        }

        log.info("Duplicate event received. Returning existing event. eventId={}",
                result.event().eventId());

        return ResponseEntity.ok(result.event());
    }

    /**
     * Returns an event using its business eventId.
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEventByEventId(
            @PathVariable String eventId) {

        log.info("Fetching event. eventId={}", eventId);

        EventResponse response =
                eventService.getEventByEventId(eventId);

        return ResponseEntity.ok(response);
    }

    /**
     * Returns all events for an account ordered by event timestamp.
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsByAccountId(
            @RequestParam("account") String accountId) {

        log.info("Fetching events for accountId={}", accountId);

        List<EventResponse> response =
                eventService.getEventsByAccountId(accountId);

        return ResponseEntity.ok(response);
    }

}