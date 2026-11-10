package com.example.wallet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/accounts")
public class WalletController {

    // Java 17 records: compact immutable request bodies (Jackson maps JSON onto them)
    record CreateAccountRequest(String ownerName) {
    }

    record DepositRequest(BigDecimal amount) {
    }

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public Account create(@RequestBody CreateAccountRequest request) {
        return walletService.createAccount(request.ownerName());
    }

    @GetMapping("/{id}")
    public Account get(@PathVariable Long id) {
        return walletService.getAccount(id);
    }

    @PostMapping("/{id}/deposit")
    public Account deposit(@PathVariable Long id, @RequestBody DepositRequest request) {
        return walletService.deposit(id, request.amount());
    }
}
