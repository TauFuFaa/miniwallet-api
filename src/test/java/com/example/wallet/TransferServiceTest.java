package com.example.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

// Integration test: real Spring context, real (in-memory H2) database.
// The class is deliberately NOT @Transactional. If it were, the service's transaction
// would join the test's transaction and the rollback test below would prove nothing.
@SpringBootTest
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accounts;

    // A spy wraps the real repository so we can force a failure in one test
    @MockitoSpyBean
    private TransferRepository transfers;

    private Long aliceId;
    private Long bobId;

    @BeforeEach
    void setUp() {
        // Clean slate for every test
        transfers.deleteAll();
        accounts.deleteAll();

        Account alice = new Account("Alice");
        alice.deposit(new BigDecimal("100.00"));
        aliceId = accounts.save(alice).getId();
        bobId = accounts.save(new Account("Bob")).getId();
    }

    private BigDecimal balanceOf(Long id) {
        return accounts.findById(id).orElseThrow().getBalance();
    }

    private void assertBalancesUnchanged() {
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("0.00");
        assertThat(transfers.count()).isZero();
    }

    @Test
    void successfulTransfer_movesMoneyAndRecordsTransfer() {
        Transfer result = transferService.transfer("k1", aliceId, bobId, new BigDecimal("30.00"));

        assertThat(result.getId()).isNotNull();
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("30.00");
        assertThat(transfers.count()).isEqualTo(1);
    }

    @Test
    void insufficientBalance_throwsAndChangesNothing() {
        assertThatThrownBy(() -> transferService.transfer("k2", aliceId, bobId, new BigDecimal("500.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient balance");

        assertBalancesUnchanged();
    }

    @Test
    void unknownAccount_throwsAndChangesNothing() {
        assertThatThrownBy(() -> transferService.transfer("k3", aliceId, 9999L, new BigDecimal("10.00")))
                .isInstanceOf(AccountNotFoundException.class);

        assertBalancesUnchanged();
    }

    @Test
    void negativeAmount_throwsAndChangesNothing() {
        assertThatThrownBy(() -> transferService.transfer("k4", aliceId, bobId, new BigDecimal("-5.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be positive");

        assertBalancesUnchanged();
    }

    @Test
    void sameAccount_isRejected() {
        assertThatThrownBy(() -> transferService.transfer("k5", aliceId, aliceId, new BigDecimal("10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot transfer to the same account");

        assertBalancesUnchanged();
    }

    @Test
    void duplicateIdempotencyKey_movesMoneyOnlyOnce() {
        Transfer first = transferService.transfer("dup", aliceId, bobId, new BigDecimal("30.00"));
        Transfer second = transferService.transfer("dup", aliceId, bobId, new BigDecimal("30.00"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("70.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("30.00");
        assertThat(transfers.count()).isEqualTo(1);
    }

    @Test
    void reusedKeyWithDifferentPayload_isRejected() {
        transferService.transfer("reuse", aliceId, bobId, new BigDecimal("30.00"));

        assertThatThrownBy(() -> transferService.transfer("reuse", aliceId, bobId, new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different request");

        assertThat(balanceOf(aliceId)).isEqualByComparingTo("70.00");
    }

    @Test
    void failureAfterBalancesChanged_rollsEverythingBack() {
        // Force the final step (saving the transfer record) to fail.
        // First flush the pending balance updates to the DB, so the debit and credit
        // really are written inside the transaction before the failure happens.
        doAnswer(invocation -> {
            accounts.flush();
            throw new RuntimeException("simulated failure");
        }).when(transfers).save(any());

        assertThatThrownBy(() -> transferService.transfer("boom", aliceId, bobId, new BigDecimal("30.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("simulated failure");

        // @Transactional rolled the already-flushed debit and credit back
        assertThat(balanceOf(aliceId)).isEqualByComparingTo("100.00");
        assertThat(balanceOf(bobId)).isEqualByComparingTo("0.00");
    }
}
