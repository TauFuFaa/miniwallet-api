package com.example.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;

    public TransferService(AccountRepository accounts, TransferRepository transfers) {
        this.accounts = accounts;
        this.transfers = transfers;
    }

    // One database transaction: debit, credit and the transfer record
    // all commit together, or (on any RuntimeException) none of them do.
    @Transactional
    public Transfer transfer(String idempotencyKey, Long fromId, Long toId, BigDecimal amount) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("Both account ids are required");
        }

        // Idempotency: same key seen before -> return the original result, move no money
        Optional<Transfer> existing = transfers.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transfer previous = existing.get();
            if (!previous.sameRequest(fromId, toId, amount)) {
                throw new IllegalArgumentException("Idempotency-Key was already used with a different request");
            }
            return previous;
        }

        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account from = accounts.findById(fromId).orElseThrow(() -> new AccountNotFoundException(fromId));
        Account to = accounts.findById(toId).orElseThrow(() -> new AccountNotFoundException(toId));

        from.withdraw(amount); // throws if amount invalid or balance too low
        to.deposit(amount);

        return transfers.save(new Transfer(idempotencyKey, fromId, toId, amount));
    }

    @Transactional(readOnly = true)
    public List<Transfer> history(Long accountId) {
        if (!accounts.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transfers.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId);
    }
}
