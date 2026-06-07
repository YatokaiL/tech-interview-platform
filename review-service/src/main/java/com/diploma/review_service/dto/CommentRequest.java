package com.diploma.review_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @Min(1) Integer lineNumber,
        @NotBlank @Size(max = 2000) String text
) {}
