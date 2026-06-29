package com.ewallet.controller;

import com.ewallet.model.Transaction;
import com.ewallet.model.User;
import com.ewallet.service.WalletService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@RestController
public class WalletController {

    @Autowired
    private WalletService walletService;

    private ResponseEntity<String> serveHtml(String filename) throws IOException {
        ClassPathResource res = new ClassPathResource(filename);
        String html = StreamUtils.copyToString(res.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private User getSessionUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        return walletService.findById(userId).orElse(null);
    }

    @GetMapping("/")
    public ResponseEntity<String> index() throws IOException { return serveHtml("index.html"); }

    @GetMapping("/signup")
    public ResponseEntity<String> signupPage() throws IOException { return serveHtml("signup.html"); }

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestParam String fullName,
                                       @RequestParam String email,
                                       @RequestParam String phone,
                                       @RequestParam String pin) {
        try {
            if (!pin.matches("\\d{4,6}"))
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "PIN must be 4-6 digits."));
            User user = walletService.register(fullName, email, phone, pin);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Registration successful! Please login.", "userId", user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/login")
    public ResponseEntity<?> login(@RequestParam String phone,
                                    @RequestParam String pin,
                                    HttpSession session) {
        Optional<User> opt = walletService.findByPhone(phone);
        if (opt.isPresent() && walletService.checkPin(opt.get(), pin)) {
            session.setAttribute("userId", opt.get().getId());
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Login successful"));
        }
        return ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "Invalid phone number or PIN"));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(java.util.Map.of("success", true));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> dashboard(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("dashboard.html");
    }

    @GetMapping("/cashin")
    public ResponseEntity<String> cashinPage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("cashin.html");
    }

    @GetMapping("/cashout")
    public ResponseEntity<String> cashoutPage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("cashout.html");
    }

    @GetMapping("/inbox")
    public ResponseEntity<String> inboxPage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("inbox.html");
    }

    @GetMapping("/profile")
    public ResponseEntity<String> profilePage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("profile.html");
    }

    @GetMapping("/delete")
    public ResponseEntity<String> deletePage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("delete.html");
    }

    @GetMapping("/barcode")
    public ResponseEntity<String> barcodePage(HttpSession session) throws IOException {
        if (getSessionUser(session) == null) return ResponseEntity.status(302).header("Location", "/").build();
        return serveHtml("barcode.html");
    }

    @GetMapping("/api/me")
    public ResponseEntity<?> getMe(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        return ResponseEntity.ok(java.util.Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "balance", user.getBalance(),
                "createdAt", user.getCreatedAt().toString()
        ));
    }

    @PostMapping("/api/cashin")
    public ResponseEntity<?> doCashIn(@RequestParam BigDecimal amount,
                                       @RequestParam(required = false) String description,
                                       HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        try {
            Transaction tx = walletService.cashIn(user, amount, description);
            user = walletService.findById(user.getId()).orElse(user);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true, "message", "Cash in successful",
                    "referenceNo", tx.getReferenceNo(),
                    "amount", tx.getAmount(),
                    "newBalance", user.getBalance()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/api/cashout")
    public ResponseEntity<?> doCashOut(@RequestParam BigDecimal amount,
                                        @RequestParam(required = false) String description,
                                        @RequestParam String recipientPhone,
                                        HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        if (recipientPhone == null || recipientPhone.isBlank())
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "Recipient phone number is required."));
        try {
            Transaction tx = walletService.cashOut(user, amount, description);
            user = walletService.findById(user.getId()).orElse(user);
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true, "message", "Cash out successful",
                    "referenceNo", tx.getReferenceNo(),
                    "amount", tx.getAmount(),
                    "newBalance", user.getBalance()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<?> getTransactions(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        List<Transaction> txs = walletService.getTransactions(user);
        List<java.util.Map<String, Object>> result = txs.stream().map(tx -> {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", tx.getId());
            m.put("type", tx.getType().name());
            m.put("amount", tx.getAmount());
            m.put("balanceBefore", tx.getBalanceBefore());
            m.put("balanceAfter", tx.getBalanceAfter());
            m.put("description", tx.getDescription());
            m.put("referenceNo", tx.getReferenceNo());
            m.put("status", tx.getStatus().name());
            m.put("createdAt", tx.getCreatedAt().toString());
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/profile")
    public ResponseEntity<?> updateProfile(@RequestParam String fullName,
                                            @RequestParam String email,
                                            @RequestParam String phone,
                                            @RequestParam(required = false) String newPin,
                                            HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        try {
            walletService.updateProfile(user, fullName, email, phone, newPin);
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Profile updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/account")
    public ResponseEntity<?> deleteAccount(HttpSession session) {
        User user = getSessionUser(session);
        if (user == null) return ResponseEntity.status(401).body(java.util.Map.of("error", "Not logged in"));
        walletService.deleteAccount(user);
        session.invalidate();
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Account deleted"));
    }
}
