package com.eventledger.gateway.dto.response;

public record CreateEventResult(

        EventResponse event,

        boolean created

) {
}