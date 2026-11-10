# MiniWallet: Transfer API

A small Java backend for a digital wallet: create accounts, deposit, transfer money between accounts, and view transfer history. Built to practise backend fundamentals relevant to payments: transactional integrity, idempotent requests, and automated testing.

**Stack:** Java 17, Spring Boot 4, Spring Web, Spring Data JPA, H2 (in-memory), JUnit 5, AssertJ, Mockito, Maven.

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/accounts` | Create an account `{"ownerName": "Alice"}` |
| GET | `/api/accounts/{id}` | Get an account and its balance |
| POST | `/api/accounts/{id}/deposit` | Deposit `{"amount": 100.50}` |
| POST | `/api/transfers` | Transfer money. Requires an `Idempotency-Key` header. Body: `{"fromAccountId": 1, "toAccountId": 2, "amount": 30.00}` |
| GET | `/api/accounts/{id}/transfers` | Transfer history for an account |

Errors return JSON `{"error": "..."}` with 400 (invalid input, insufficient balance), 404 (unknown account) or 409 (conflicting duplicate request).

## Run and test

```
./mvnw spring-boot:run     # starts on http://localhost:8080
./mvnw test                # unit and integration tests
```

## Design decisions

- **`BigDecimal` for money.** Floating-point types cannot represent amounts like 0.10 exactly.
- **Business rules in the `Account` entity.** Deposits and withdrawals validate themselves, so the rules cannot be bypassed by a caller.
- **`@Transactional` transfer.** Debit, credit and the transfer record commit together or not at all. Unchecked exceptions trigger rollback.
- **Idempotency key.** The client sends a unique key per logical transfer. A retry with the same key returns the original result and moves no money. Reusing a key with a different payload is rejected.
- **Unique constraint on the key.** Backstop for two identical requests arriving at the same moment; surfaces as a 409.
- **Central exception handling** (`@RestControllerAdvice`) maps domain exceptions to HTTP status codes.
- **Constructor injection** throughout, so dependencies are explicit and easy to test.

## Testing

- `AccountTest`: unit tests for balance rules (no Spring).
- `TransferServiceTest`: integration tests against H2 covering successful transfer, insufficient balance, unknown account, negative amount, same-account transfer, duplicate idempotency key, key reuse with a different payload, and **rollback**. The rollback test forces a failure after the balances have been flushed to the database and verifies the transaction undoes them.

## Known limitations and next steps

- **Concurrent transfers from the same account** could both pass the balance check and overdraw it. Fix: optimistic locking (`@Version` on `Account`) with retry, or pessimistic locking (`@Lock(PESSIMISTIC_WRITE)`) on the source account. Locking accounts in a consistent order avoids deadlocks.
- API returns JPA entities directly; a production version would use separate response DTOs.
- No authentication, input-validation annotations, or currency handling.
- H2 is in-memory, so data resets on restart. A real deployment would use PostgreSQL or MySQL plus a migration tool such as Flyway.

## How I used AI in this project

- **Tool and role:** I used Claude as a pairing partner for design and code generation. The code is AI-assisted: Claude generated most of it, and I ran, tested and debugged it. I also compared its project proposal against a second AI's, then chose the wallet/transfer API over a transaction-classifier idea because it exercises more of the real backend problems (transactions, retries, correctness).
- **What I asked it to do:** propose the architecture, generate the entities, services, controllers and exception handler, write the unit and integration test suite, and explain concepts such as `@Transactional`, idempotency and locking as I went.
- **What I reviewed and verified myself:**
    - Ran every endpoint manually with PowerShell: create account, deposit, transfer, history.
    - Tested the error paths: negative amount, unknown account, insufficient balance.
    - Sent the same transfer twice with the same `Idempotency-Key` and confirmed the second returned the original transfer and balances changed only once (Alice 70.50, Bob 30.00).
    - Ran the full test suite with Maven and confirmed it passes.
- **What went wrong or needed fixing:**
    - PowerShell mangled the JSON quotes with `curl`, so I switched to `Invoke-RestMethod` and sent JSON files for the error cases.
    - I had picked Java 21 on Spring Initializr but my JDK was 17, so I changed it to 17 to match.
    - The project was committed one folder too deep and included IDE files and scratch JSON files, so I restructured the repo and removed them.
    - IntelliJ's Maven import failed with the Maven wrapper, so I switched it to the bundled Maven. The terminal build was unaffected.
- **What I changed or would change:** I chose the project scope, the Java version and the decision to skip an LLM integration in favour of correctness and tests. Next I would add locking for concurrent transfers, response DTOs instead of returning entities, input validation, and a real database with migrations.
- **What I learned:**
    - `@Transactional` rolls back on unchecked exceptions and gives all-or-nothing per request, but it doesn't stop two concurrent requests overdrawing an account. That needs optimistic or pessimistic locking.
    - Idempotency needs both an application check and a unique database constraint, because two identical requests can arrive at the same instant.
    - A rollback test must not run inside a test-managed transaction, or it proves nothing.
    - Money must be `BigDecimal`, not `double`.