package com.example.wallet;

// Unchecked (extends RuntimeException) so callers aren't forced to catch it;
// the exception handler turns it into a 404.
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        super("Account not found: " + id);
    }
}
