package com.ewallet.service;

import com.ewallet.model.Transaction;
import com.ewallet.model.User;

import java.math.BigDecimal;

public interface WalletOperation {
    Transaction execute(User user, BigDecimal amount, String description);
}
