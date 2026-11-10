package com.example.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    // Spring Data builds the SQL from the method name
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    List<Transfer> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromAccountId, Long toAccountId);
}
