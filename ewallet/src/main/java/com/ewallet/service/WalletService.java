package com.ewallet.service;

import com.ewallet.model.Transaction;
import com.ewallet.model.User;
import com.ewallet.repository.TransactionRepository;
import com.ewallet.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class WalletService {

    @Autowired private UserRepository userRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private CashInOperation cashInOperation;
    @Autowired private CashOutOperation cashOutOperation;
    @Autowired private PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String fullName, String email, String phone, String pin) {
        if (userRepository.existsByPhone(phone))
            throw new IllegalArgumentException("Phone number already registered.");
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered.");
        User user = new User(fullName, email, phone, passwordEncoder.encode(pin));
        return userRepository.save(user);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public boolean checkPin(User user, String rawPin) {
        return passwordEncoder.matches(rawPin, user.getPin());
    }

    public Transaction cashIn(User user, BigDecimal amount, String description) {
        return cashInOperation.execute(user, amount, description);
    }

    // ✅ CHANGED: third param is now recipientPhone (not description)
    public Transaction cashOut(User user, BigDecimal amount, String recipientPhone) {
        return cashOutOperation.execute(user, amount, recipientPhone);
    }

    public List<Transaction> getTransactions(User user) {
        return transactionRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Transaction> getRecentTransactions(User user) {
        return transactionRepository.findTop10ByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public User updateProfile(User user, String fullName, String email, String phone, String newPin) {
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already in use.");
        if (!user.getPhone().equals(phone) && userRepository.existsByPhone(phone))
            throw new IllegalArgumentException("Phone already in use.");
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        if (newPin != null && !newPin.isBlank()) {
            if (!newPin.matches("\\d{4,6}"))
                throw new IllegalArgumentException("PIN must be 4-6 digits.");
            user.setPin(passwordEncoder.encode(newPin));
        }
        return userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(User user) {
        transactionRepository.deleteAll(transactionRepository.findByUserOrderByCreatedAtDesc(user));
        userRepository.delete(user);
    }
}
