package com.ewallet.repository;

import com.ewallet.model.Transaction;
import com.ewallet.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByUserAndTypeOrderByCreatedAtDesc(User user, Transaction.Type type);
    Optional<Transaction> findByReferenceNo(String referenceNo);
    List<Transaction> findTop10ByUserOrderByCreatedAtDesc(User user);
}
