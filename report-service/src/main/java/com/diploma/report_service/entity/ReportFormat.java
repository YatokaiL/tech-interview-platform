package com.diploma.report_service.entity;

public enum ReportFormat {
    PDF("application/pdf", "pdf"),
    JSON("application/json", "json");

    private final String mediaType;
    private final String extension;

    ReportFormat(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String getMediaType() { return mediaType; }
    public String getExtension() { return extension; }
}
