package com.diploma.report_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JsonExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);

    public byte[] render(ReportContext ctx) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("generatedAt", Instant.now().toString());
        root.set("session", MAPPER.valueToTree(ctx.session()));
        root.set("review", MAPPER.valueToTree(ctx.review()));
        if (ctx.snapshot() != null) {
            root.set("snapshot", MAPPER.valueToTree(ctx.snapshot()));
        }
        if (ctx.interviewer() != null) {
            root.set("interviewer", MAPPER.valueToTree(ctx.interviewer()));
        }
        if (ctx.candidate() != null) {
            root.set("candidate", MAPPER.valueToTree(ctx.candidate()));
        }
        try {
            return MAPPER.writeValueAsString(root).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to render JSON report", e);
        }
    }
}
