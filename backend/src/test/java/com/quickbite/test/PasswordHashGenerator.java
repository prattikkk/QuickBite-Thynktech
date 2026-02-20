package com.quickbite.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
        String password = "Test@1234";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        
        // Verify the hash from fix_passwords.sql
        String existingHash = "$2a$10$maNiaEtOZzJupIB9rVzxf.RNmNNfu3P0gxUeewM/tio5WVYrcWzXO";
        boolean matches = encoder.matches(password, existingHash);
        System.out.println("Matches existing hash: " + matches);
    }
}
