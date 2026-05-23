package com.diploma.execution_service.entity;

public enum ExecutionStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILURE,
    TIMEOUT,
    ERROR;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILURE || this == TIMEOUT || this == ERROR;
    }
}
