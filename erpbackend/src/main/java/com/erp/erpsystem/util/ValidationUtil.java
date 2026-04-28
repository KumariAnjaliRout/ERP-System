package com.erp.erpsystem.util;

public class ValidationUtil {

    private static final String SAFE_ID_PATTERN = "^[a-zA-Z0-9\\-_]+$";
    private static final int MAX_ID_LENGTH = 100;

    public static void validateId(String id, String fieldName) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        if (id.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(fieldName + " is too long");
        }
        if (!id.matches(SAFE_ID_PATTERN)) {
            throw new IllegalArgumentException(
                    fieldName + " contains invalid characters");
        }
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("[^a-zA-Z0-9\\s\\-_@.,]", "");
    }
}