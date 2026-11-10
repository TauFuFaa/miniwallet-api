package com.example.wallet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final AccountRepository accounts;

    // Constructor injection: Spring supplies the repository.
    // Preferred over field @Autowired because the dependency is explicit, final, and easy to mock in tests.
    public WalletService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public Account createAccount(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name is required");
        }
        return accounts.save(new Account(ownerName));
    }

    public Account getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    // Load, change, save happen in one database transaction:
    // if anything throws, the whole thing rolls back.
    @Transactional
    public Account deposit(Long id, BigDecimal amount) {
        Account account = getAccount(id);
        account.deposit(amount);
        return account; // JPA flushes the change to the DB on commit
    }
}
