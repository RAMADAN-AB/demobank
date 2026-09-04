package com.demobank.api.repository;

import com.demobank.api.model.Account;
import com.demobank.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySourceAccountOrTargetAccountOrderByTimestampDesc(Account source, Account target);
}
