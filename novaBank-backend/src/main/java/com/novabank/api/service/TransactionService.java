package com.novabank.api.service;

import com.novabank.api.dto.TransactionResponse;
import com.novabank.api.dto.TransferRequest;
import com.novabank.api.model.Account;
import com.novabank.api.model.Transaction;
import com.novabank.api.repository.AccountRepository;
import com.novabank.api.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Charger et vérifier le compte émetteur
        Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                .orElseThrow(() -> new RuntimeException("Compte source introuvable"));

        if (!sourceAccount.getUser().getEmail().equals(email)) {
            throw new RuntimeException("Vous n'êtes pas propriétaire de ce compte");
        }

        // 2. Vérifier le montant
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Le montant doit être supérieur à zéro");
        }

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Solde insuffisant pour effectuer le virement");
        }

        // 3. Charger le compte destinataire
        Account targetAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new RuntimeException("Compte destinataire introuvable"));

        if (sourceAccount.getAccountNumber().equals(targetAccount.getAccountNumber())) {
            throw new RuntimeException("Impossible d'effectuer un virement vers le même compte");
        }

        // 4. Exécuter le mouvement de fonds (ACID)
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));
        targetAccount.setBalance(targetAccount.getBalance().add(request.getAmount()));

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 5. Enregistrer l'historique
        Transaction transaction = Transaction.builder()
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(request.getAmount())
                .description(request.getDescription())
                .timestamp(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        return TransactionResponse.builder()
                .id(saved.getId())
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .timestamp(saved.getTimestamp())
                .sourceAccount(sourceAccount.getAccountNumber())
                .targetAccount(targetAccount.getAccountNumber())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Compte introuvable"));

        return transactionRepository.findBySourceAccountOrTargetAccountOrderByTimestampDesc(account, account)
                .stream()
                .map(t -> TransactionResponse.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .description(t.getDescription())
                        .timestamp(t.getTimestamp())
                        .sourceAccount(t.getSourceAccount().getAccountNumber())
                        .targetAccount(t.getTargetAccount().getAccountNumber())
                        .build())
                .toList();
    }
}
