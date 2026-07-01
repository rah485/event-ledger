package com.eventledger.gateway.mapper;

import com.eventledger.gateway.dto.request.EventRequest;
import com.eventledger.gateway.dto.request.MetadataRequest;
import com.eventledger.gateway.dto.response.EventResponse;
import com.eventledger.gateway.entity.EventEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class EventMapper {

    private final ObjectMapper objectMapper;

    public EventMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public EventEntity toEntity(EventRequest request) {

        Objects.requireNonNull(request, "EventRequest cannot be null");

        EventEntity entity = new EventEntity();

        entity.setEventId(request.eventId());
        entity.setAccountId(request.accountId());
        entity.setType(request.type());
        entity.setAmount(request.amount());
        entity.setCurrency(request.currency());
        entity.setEventTimestamp(request.eventTimestamp());

        try {
            if (request.metadata() != null) {
                entity.setMetadata(
                        objectMapper.writeValueAsString(request.metadata()));
            }
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Unable to serialize metadata.", ex);
        }

        return entity;
    }

    public EventResponse toResponse(EventEntity entity) {

        Objects.requireNonNull(entity, "EventEntity cannot be null");

        MetadataRequest metadata = null;

        try {
            if (StringUtils.hasText(entity.getMetadata())) {
                metadata = objectMapper.readValue(
                        entity.getMetadata(),
                        MetadataRequest.class);
            }
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Unable to deserialize metadata.", ex);
        }

        return new EventResponse(
                entity.getEventId(),
                entity.getAccountId(),
                entity.getType(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getEventTimestamp(),
                metadata
        );
    }
}