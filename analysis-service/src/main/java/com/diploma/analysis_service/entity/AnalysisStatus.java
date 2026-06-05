package com.diploma.analysis_service.entity;

public enum AnalysisStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    ERROR;

    public boolean isTerminal() {
        return this == COMPLETED || this == ERROR;
    }
}
