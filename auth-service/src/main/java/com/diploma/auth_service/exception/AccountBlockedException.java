package com.diploma.auth_service.exception;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException() {
        super("Account is blocked");
    }
}
