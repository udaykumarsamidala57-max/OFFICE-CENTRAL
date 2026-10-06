package com.Inventory.SERVICE;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.Inventory.Bean.UserAccount;
import com.Inventory.DAO.LoginDAO;

@Service
public class LoginService {

    private final LoginDAO loginDAO;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginService(LoginDAO loginDAO) {
        this.loginDAO = loginDAO;
    }

    public Optional<UserAccount> authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserAccount> candidate = loginDAO.findByUsername(username.trim());
        if (candidate.isEmpty()) return Optional.empty();

        UserAccount user = candidate.get();
        String stored = user.getPassword();
        if (stored == null || !passwordMatches(password, stored)) return Optional.empty();

        user.setPassword(null);
        return Optional.of(user);
    }

    private boolean passwordMatches(String supplied, String stored) {
        if (isBcryptHash(stored)) {
            try {
                return passwordEncoder.matches(supplied, stored);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
        return MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
