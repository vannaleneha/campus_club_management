package com.campusclub.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility providing secure password hashing and verification using BCrypt.
 * Features backwards compatibility for existing plain-text passwords and transparent migration.
 */
public class PasswordUtil {

    private static final int BCRYPT_LOG_ROUNDS = 10;

    /**
     * Hashes a plain-text password using BCrypt with a secure random salt.
     *
     * @param plainPassword the plain-text password
     * @return the BCrypt-hashed password string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_LOG_ROUNDS));
    }

    /**
     * Verifies a plain-text password against a stored password or hash.
     * If the stored value is a valid BCrypt hash, it verifies using BCrypt.
     * If the stored value is an unmigrated plain-text password, it compares strings directly.
     *
     * @param plainPassword     the plain-text candidate password
     * @param storedHashOrPlain the password or hash stored in the database
     * @return true if password matches; false otherwise
     */
    public static boolean checkPassword(String plainPassword, String storedHashOrPlain) {
        if (plainPassword == null || storedHashOrPlain == null) {
            return false;
        }

        if (isBCryptHash(storedHashOrPlain)) {
            try {
                return BCrypt.checkpw(plainPassword, storedHashOrPlain);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        // Backward-compatible fallback for unmigrated legacy plain-text passwords
        return plainPassword.equals(storedHashOrPlain);
    }

    /**
     * Checks whether a stored string has a valid BCrypt hash prefix ($2a$, $2b$, or $2y$)
     * and minimum length (typically 60 characters).
     *
     * @param value the string to check
     * @return true if it is a BCrypt hash; false otherwise
     */
    public static boolean isBCryptHash(String value) {
        if (value == null || value.length() < 60) {
            return false;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}
