package org.example.safenest.exception;

public class CustomException {
    public static class UserAlreadyExistsException extends ApiException {
        public UserAlreadyExistsException(String email) {
            super("User with email " + email + " already exists", 409);
        }
    }

    public static class ResourceNotFoundException extends ApiException {
        public ResourceNotFoundException(String resource) {
            super(resource + " not found", 404);
        }
    }

    public static class InsufficientBalanceException extends ApiException {
        public InsufficientBalanceException(String currency) {
            super("Insufficient balance for " + currency, 400);
        }
    }
}