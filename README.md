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

> Fill this in with what actually happened. Specifics are what make it credible.

- **Tool and role:** I used Claude as a pairing partner for design and code generation. (Be upfront that the code was AI-assisted.)
- **What I asked it to do:** *(e.g. propose the architecture, generate entities/services/controllers, draft the test suite)*
- **What I reviewed and verified myself:** *(e.g. ran every endpoint manually with PowerShell, checked that a retry with the same key moved money once, confirmed all tests pass)*
- **What went wrong or needed fixing:** *(e.g. PowerShell mangled JSON quotes in curl; Spring Boot 4 vs 3 differences; anything else you hit)*
- **What I changed or would change:** *(e.g. things you decided yourself)*
- **What I learned:** *(e.g. why rollback only applies to unchecked exceptions, why idempotency needs a DB constraint as well as a check)*
