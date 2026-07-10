package com.roze.thundercall.ui.utils;

import java.util.ArrayList;
import java.util.List;

public class Validator {
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email != null && email.matches(emailRegex);
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 4;
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.matches("^[a-zA-Z0-9_]+$");
    }

    public static List<String> validateRegistration(String username, String email, String password) {
        List<String> errors = new ArrayList<>();
        if (!isValidUsername(username)) {
            errors.add("Username must be at least 3 characters and contain only letters,numbers and underscores");
        }
        if (!isValidEmail(email)) {
            errors.add("Please enter a valid email address");
        }
        if (!isValidPassword(password)) {
            errors.add("Password must be at least 4 characters long");
        }
        return errors;
    }
}
