package com.example.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransferController {

    record TransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount) {
    }

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    // The client sends a unique key per logical transfer (usually a UUID) in a header,
    // and reuses the same key when retrying.
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer transfer(@RequestHeader("Idempotency-Key") String idempotencyKey,
                             @RequestBody TransferRequest request) {
        return transferService.transfer(idempotencyKey,
                request.fromAccountId(), request.toAccountId(), request.amount());
    }

    @GetMapping("/accounts/{id}/transfers")
    public List<Transfer> history(@PathVariable Long id) {
        return transferService.history(id);
    }
}
