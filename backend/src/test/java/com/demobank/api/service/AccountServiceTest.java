package com.demobank.api.service;

import com.demobank.api.dto.AccountResponse;
import com.demobank.api.model.Account;
import com.demobank.api.model.User;
import com.demobank.api.repository.AccountRepository;
import com.demobank.api.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    private static final String EMAIL = "alice@demobank.test";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of()));
        user = User.builder().email(EMAIL).password("x").build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAccount_opensAccountWithWelcomeBalanceForCurrentUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        AccountResponse response = accountService.createAccount();

        ArgumentCaptor<Account> saved = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.getAccountNumber()).matches("FR76[0-9A-F]{10}");
    }

    @Test
    void createAccount_generatesDistinctAccountNumbers() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        String first = accountService.createAccount().getAccountNumber();
        String second = accountService.createAccount().getAccountNumber();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void createAccount_failsWhenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount())
                .hasMessage("Utilisateur introuvable");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getMyAccounts_returnsOnlyCurrentUserAccounts() {
        user.setAccounts(List.of(
                Account.builder().id(1L).accountNumber("FR76AAAAAAAAAA").balance(new BigDecimal("10.00")).user(user).build(),
                Account.builder().id(2L).accountNumber("FR76BBBBBBBBBB").balance(new BigDecimal("20.00")).user(user).build()
        ));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        List<AccountResponse> accounts = accountService.getMyAccounts();

        assertThat(accounts).extracting(AccountResponse::getAccountNumber)
                .containsExactly("FR76AAAAAAAAAA", "FR76BBBBBBBBBB");
        assertThat(accounts.get(1).getBalance()).isEqualByComparingTo("20.00");
    }

    @Test
    void getMyAccounts_failsWhenUserNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getMyAccounts())
                .hasMessage("Utilisateur introuvable");
    }
}
