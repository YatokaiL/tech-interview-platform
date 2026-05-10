package com.diploma.session_service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
