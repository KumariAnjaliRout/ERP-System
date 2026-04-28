package com.erp.erpsystem.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class PasswordPolicyService {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    private static final Pattern DIGIT_PATTERN         = Pattern.compile("[0-9]");
    private static final Pattern UPPERCASE_PATTERN     = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN     = Pattern.compile("[a-z]");
    private static final Pattern SPECIAL_CHAR_PATTERN  = Pattern.compile("[^a-zA-Z0-9]");
     private static final Pattern WHITESPACE_PATTERN    = Pattern.compile("\\s");

    private static final String[] WEAK_PASSWORD_BASES = {
            "password", "123456", "qwerty", "abc123",
            "iloveyou", "admin", "welcome", "monkey",
            "dragon", "master", "letmein", "login",
            "superman", "batman", "trustno1", "shadow"
    };

    public void validatePassword(String password) {
        // FIX: check null and blank separately — don't trim the original
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Password must be at least %d characters long", MIN_LENGTH));
        }

        if (password.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Password cannot exceed %d characters", MAX_LENGTH));
        }

        // FIX: no spaces allowed
        if (WHITESPACE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("Password must not contain spaces");
        }

        // FIX: require uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter");
        }

        // FIX: require lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one lowercase letter");
        }

        // Require digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one digit");
        }

        // FIX: require special character — important for ERP security
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character (e.g. @, #, !)");
        }

        // FIX: check if password CONTAINS a weak base — not just exact match
        // "Password1!" still fails because it contains "password"
        String lowerPassword = password.toLowerCase();
        for (String weak : WEAK_PASSWORD_BASES) {
            if (lowerPassword.contains(weak)) {
                throw new IllegalArgumentException(
                        "Password is too common or weak. Avoid common words like '" + weak + "'");
            }
        }
    }

    public String getPolicyDescription() {
        return String.format(
                "Password must be %d-%d characters, contain at least one uppercase letter, " +
                        "one lowercase letter, one digit, one special character, and no spaces.",
                MIN_LENGTH, MAX_LENGTH);
    }
}