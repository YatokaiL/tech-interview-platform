package com.diploma.execution_service.runner;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class LanguageProfiles {

    private final Map<String, LanguageProfile> byKey = new LinkedHashMap<>();

    public LanguageProfiles() {
        register(new LanguageProfile(
                "java",
                "eclipse-temurin:21-jdk-alpine",
                "Main.java",
                "javac /sandbox/Main.java -d /sandbox && cd /sandbox && java Main"
        ));
        register(new LanguageProfile(
                "python",
                "python:3.12-alpine",
                "main.py",
                "python /sandbox/main.py"
        ));
        register(new LanguageProfile(
                "javascript",
                "node:20-alpine",
                "main.js",
                "node /sandbox/main.js"
        ));
        register(new LanguageProfile(
                "go",
                "golang:1.22-alpine",
                "main.go",
                "cd /sandbox && go run main.go"
        ));
        register(new LanguageProfile(
                "cpp",
                "gcc:13",
                "main.cpp",
                "g++ -O0 -std=c++17 /sandbox/main.cpp -o /sandbox/main && /sandbox/main"
        ));
    }

    private void register(LanguageProfile p) {
        byKey.put(p.key(), p);
    }

    public Optional<LanguageProfile> find(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(byKey.get(key.toLowerCase()));
    }

    public java.util.Collection<LanguageProfile> all() {
        return byKey.values();
    }
}
