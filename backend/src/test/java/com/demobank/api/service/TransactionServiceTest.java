package com.demobank.api.service;

import com.demobank.api.dto.TransactionResponse;
import com.demobank.api.dto.TransferRequest;
import com.demobank.api.model.Account;
import com.demobank.api.model.Transaction;
import com.demobank.api.model.User;
import com.demobank.api.repository.AccountRepository;
import com.demobank.api.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String OWNER_EMAIL = "alice@demobank.test";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, null, List.of()));

        source = account("FR76SOURCE0001", "500.00", user(OWNER_EMAIL));
        target = account("FR76TARGET0001", "100.00", user("bob@demobank.test"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- transfer ----------

    @Test
    void transfer_debitsSourceCreditsTargetAndRecordsTransaction() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("FR76TARGET0001")).thenReturn(Optional.of(target));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        TransactionResponse response = transactionService.transfer(request("FR76SOURCE0001", "FR76TARGET0001", "150.00"));

        assertThat(source.getBalance()).isEqualByComparingTo("350.00");
        assertThat(target.getBalance()).isEqualByComparingTo("250.00");
        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getAmount()).isEqualByComparingTo("150.00");
        assertThat(response.getSourceAccount()).isEqualTo("FR76SOURCE0001");
        assertThat(response.getTargetAccount()).isEqualTo("FR76TARGET0001");

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getDescription()).isEqualTo("Loyer");
        assertThat(saved.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void transfer_allowsTransferOfEntireBalance() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("FR76TARGET0001")).thenReturn(Optional.of(target));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionService.transfer(request("FR76SOURCE0001", "FR76TARGET0001", "500.00"));

        assertThat(source.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void transfer_failsWhenSourceAccountNotFound() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(request("UNKNOWN", "FR76TARGET0001", "10.00")))
                .hasMessage("Compte source introuvable");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenCallerDoesNotOwnSourceAccount() {
        when(accountRepository.findByAccountNumber("FR76TARGET0001")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> transactionService.transfer(request("FR76TARGET0001", "FR76SOURCE0001", "10.00")))
                .hasMessage("Vous n'êtes pas propriétaire de ce compte");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenAmountIsZero() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> transactionService.transfer(request("FR76SOURCE0001", "FR76TARGET0001", "0")))
                .hasMessage("Le montant doit être supérieur à zéro");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenAmountIsNegative() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> transactionService.transfer(request("FR76SOURCE0001", "FR76TARGET0001", "-50.00")))
                .hasMessage("Le montant doit être supérieur à zéro");
        assertThat(source.getBalance()).isEqualByComparingTo("500.00");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenBalanceIsInsufficient() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> transactionService.transfer(request("FR76SOURCE0001", "FR76TARGET0001", "500.01")))
                .hasMessage("Solde insuffisant pour effectuer le virement");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenTargetAccountNotFound() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer(request("FR76SOURCE0001", "UNKNOWN", "10.00")))
                .hasMessage("Compte destinataire introuvable");
        assertNothingSaved();
    }

    @Test
    void transfer_failsWhenSourceAndTargetAreTheSameAccount() {
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> transactionService.transfer(request("FR76SOURCE0001", "FR76SOURCE0001", "10.00")))
                .hasMessage("Impossible d'effectuer un virement vers le même compte");
        assertThat(source.getBalance()).isEqualByComparingTo("500.00");
        assertNothingSaved();
    }

    // ---------- getHistory ----------

    @Test
    void getHistory_returnsMappedTransactionsForOwner() {
        Transaction t = Transaction.builder()
                .id(7L)
                .sourceAccount(source)
                .targetAccount(target)
                .amount(new BigDecimal("25.00"))
                .description("Cafe")
                .timestamp(LocalDateTime.of(2026, 1, 15, 10, 0))
                .build();
        when(accountRepository.findByAccountNumber("FR76SOURCE0001")).thenReturn(Optional.of(source));
        when(transactionRepository.findBySourceAccountOrTargetAccountOrderByTimestampDesc(source, source))
                .thenReturn(List.of(t));

        List<TransactionResponse> history = transactionService.getHistory("FR76SOURCE0001");

        assertThat(history).hasSize(1);
        TransactionResponse r = history.get(0);
        assertThat(r.getId()).isEqualTo(7L);
        assertThat(r.getAmount()).isEqualByComparingTo("25.00");
        assertThat(r.getDescription()).isEqualTo("Cafe");
        assertThat(r.getSourceAccount()).isEqualTo("FR76SOURCE0001");
        assertThat(r.getTargetAccount()).isEqualTo("FR76TARGET0001");
    }

    @Test
    void getHistory_failsWhenCallerDoesNotOwnAccount() {
        when(accountRepository.findByAccountNumber("FR76TARGET0001")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> transactionService.getHistory("FR76TARGET0001"))
                .hasMessage("Vous n'êtes pas propriétaire de ce compte");
        verify(transactionRepository, never()).findBySourceAccountOrTargetAccountOrderByTimestampDesc(any(), any());
    }

    @Test
    void getHistory_failsWhenAccountNotFound() {
        when(accountRepository.findByAccountNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getHistory("UNKNOWN"))
                .hasMessage("Compte introuvable");
    }

    // ---------- helpers ----------

    private void assertNothingSaved() {
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    private static User user(String email) {
        return User.builder().email(email).password("x").build();
    }

    private static Account account(String number, String balance, User owner) {
        return Account.builder().accountNumber(number).balance(new BigDecimal(balance)).user(owner).build();
    }

    private static TransferRequest request(String from, String to, String amount) {
        return TransferRequest.builder()
                .sourceAccountNumber(from)
                .targetAccountNumber(to)
                .amount(new BigDecimal(amount))
                .description("Loyer")
                .build();
    }
}
