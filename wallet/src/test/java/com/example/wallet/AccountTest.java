package com.example.wallet;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Pure unit tests: no Spring, no database, runs in milliseconds
class AccountTest {

    @Test
    void deposit_increasesBalance() {
        Account account = new Account("Alice");

        account.deposit(new BigDecimal("25.50"));

        assertThat(account.getBalance()).isEqualByComparingTo("25.50");
    }

    @Test
    void withdraw_decreasesBalance() {
        Account account = new Account("Alice");
        account.deposit(new BigDecimal("100.00"));

        account.withdraw(new BigDecimal("40.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    void withdraw_moreThanBalance_throwsAndLeavesBalanceUnchanged() {
        Account account = new Account("Alice");
        account.deposit(new BigDecimal("10.00"));

        assertThatThrownBy(() -> account.withdraw(new BigDecimal("10.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");
        assertThat(account.getBalance()).isEqualByComparingTo("10.00");
    }

    @Test
    void deposit_zeroOrNegative_throws() {
        Account account = new Account("Alice");

        assertThatThrownBy(() -> account.deposit(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> account.deposit(new BigDecimal("-5")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
