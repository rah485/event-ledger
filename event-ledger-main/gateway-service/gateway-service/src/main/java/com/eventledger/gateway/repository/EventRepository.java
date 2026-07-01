package com.eventledger.gateway.repository;

import com.eventledger.gateway.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    /**
     * Find an event using the business eventId.
     * Used for idempotency.
     */
    Optional<EventEntity> findByEventId(String eventId);

    /**
     * Check whether an event already exists.
     * Used before saving a new event.
     */
    boolean existsByEventId(String eventId);

    /**
     * Retrieve all events for an account ordered by event timestamp.
     */
    List<EventEntity> findByAccountIdOrderByEventTimestampAsc(String accountId);

}