package com.ewallet.service;

import com.ewallet.model.Transaction;
import com.ewallet.model.User;
import com.ewallet.repository.TransactionRepository;
import com.ewallet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CashInOperation implements WalletOperation {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction execute(User user, BigDecimal amount, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cash-in amount must be greater than zero.");
        }

        BigDecimal balanceBefore = user.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        user.setBalance(balanceAfter);
        userRepository.save(user);

        String refNo = generateReference("CI");
        Transaction tx = new Transaction(user, Transaction.Type.CASH_IN, amount,
                balanceBefore, balanceAfter,
                description != null ? description : "Cash In",
                refNo);
        tx.setStatus(Transaction.Status.SUCCESS);
        return transactionRepository.save(tx);
    }

    private String generateReference(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + timestamp + "-" + uuid;
    }
}
