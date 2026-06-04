package com.diploma.analysis_service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String type, Object id) {
        super(type + " not found: " + id);
    }
}
