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
public class CashOutOperation implements WalletOperation {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction execute(User sender, BigDecimal amount, String recipientPhone) {
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Cash-out amount must be greater than zero.");

        // Validate sender balance
        if (sender.getBalance().compareTo(amount) < 0)
            throw new IllegalArgumentException("Insufficient balance.");

        // Find recipient by phone number
        User recipient = userRepository.findByPhone(recipientPhone)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account found with phone number: " + recipientPhone));

        // Prevent sending to yourself
        if (sender.getPhone().equals(recipientPhone))
            throw new IllegalArgumentException("You cannot send money to yourself.");

        // Shared reference number — same base for both sides
        String refNo = generateReference("CO");
        LocalDateTime now = LocalDateTime.now();

        // ── SENDER: deduct balance ───────────────────────────────
        BigDecimal senderBalBefore = sender.getBalance();
        BigDecimal senderBalAfter  = senderBalBefore.subtract(amount);
        sender.setBalance(senderBalAfter);
        userRepository.save(sender);

        // Sender's history shows: "Received by Juan Dela Cruz · 09123456789"
        String senderDesc = "Received by " + recipient.getFullName()
                + " · " + recipient.getPhone();

        Transaction senderTx = new Transaction(
                sender,
                Transaction.Type.CASH_OUT,
                amount,
                senderBalBefore,
                senderBalAfter,
                senderDesc,
                refNo
        );
        senderTx.setStatus(Transaction.Status.SUCCESS);
        senderTx.setCreatedAt(now);
        transactionRepository.save(senderTx);

        // ── RECIPIENT: add balance ───────────────────────────────
        BigDecimal recipBalBefore = recipient.getBalance();
        BigDecimal recipBalAfter  = recipBalBefore.add(amount);
        recipient.setBalance(recipBalAfter);
        userRepository.save(recipient);

        // Recipient's history shows: "Transferred by Maria Santos · 09987654321"
        String recipDesc = "Transferred by " + sender.getFullName()
                + " · " + sender.getPhone();

        Transaction recipientTx = new Transaction(
                recipient,
                Transaction.Type.CASH_IN,
                amount,
                recipBalBefore,
                recipBalAfter,
                recipDesc,
                refNo + "-R"   // unique per row; "-R" marks the receiving side
        );
        recipientTx.setStatus(Transaction.Status.SUCCESS);
        recipientTx.setCreatedAt(now);
        transactionRepository.save(recipientTx);

        return senderTx; // return sender's tx to controller
    }

    private String generateReference(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + "-" + timestamp + "-" + uuid;
    }
}
