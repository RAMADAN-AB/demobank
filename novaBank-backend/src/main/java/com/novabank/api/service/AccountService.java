package com.novabank.api.service;

import com.novabank.api.dto.AccountResponse;
import com.novabank.api.model.Account;
import com.novabank.api.model.User;
import com.novabank.api.repository.AccountRepository;
import com.novabank.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Génération d'un numéro de compte unique (FR + 10 caractères)
        String accountNumber = "FR76" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();

        Account account = Account.builder()
                .accountNumber(accountNumber)
                .balance(new BigDecimal("1000.00")) // Offre de bienvenue / Solde initial
                .user(user)
                .build();

        Account saved = accountRepository.save(account);

        return AccountResponse.builder()
                .id(saved.getId())
                .accountNumber(saved.getAccountNumber())
                .balance(saved.getBalance())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        return user.getAccounts().stream()
                .map(acc -> AccountResponse.builder()
                        .id(acc.getId())
                        .accountNumber(acc.getAccountNumber())
                        .balance(acc.getBalance())
                        .build())
                .toList();
    }
}
