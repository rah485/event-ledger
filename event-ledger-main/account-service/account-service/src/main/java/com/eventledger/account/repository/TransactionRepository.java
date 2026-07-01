package com.eventledger.account.repository;

import com.eventledger.account.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByEventId(String eventId);

    List<TransactionEntity> findByAccountIdOrderByEventTimestampDesc(String accountId);
}