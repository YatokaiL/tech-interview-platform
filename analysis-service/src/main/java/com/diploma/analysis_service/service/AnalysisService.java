package com.diploma.analysis_service.service;

import com.diploma.analysis_service.analyzer.AnalysisResult;
import com.diploma.analysis_service.analyzer.Analyzer;
import com.diploma.analysis_service.analyzer.AnalyzerRegistry;
import com.diploma.analysis_service.client.SessionAccessClient;
import com.diploma.analysis_service.dto.AnalysisCompletedEvent;
import com.diploma.analysis_service.dto.AnalysisRequest;
import com.diploma.analysis_service.entity.AnalysisStatus;
import com.diploma.analysis_service.entity.CodeAnalysis;
import com.diploma.analysis_service.exception.ForbiddenException;
import com.diploma.analysis_service.exception.NotFoundException;
import com.diploma.analysis_service.kafka.AnalysisEventPublisher;
import com.diploma.analysis_service.repository.CodeAnalysisRepository;
import com.diploma.analysis_service.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final CodeAnalysisRepository repository;
    private final AnalyzerRegistry analyzerRegistry;
    private final SessionAccessClient sessionAccessClient;
    private final ExecutorService analysisExecutor;
    private final AnalysisEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Transactional
    public CodeAnalysis submit(AnalysisRequest request) {
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(request.sessionId(), CurrentUser.rawToken());
        if (info == null) {
            throw new ForbiddenException("Session is not accessible to current user");
        }

        UUID userId = CurrentUser.id();
        if (!CurrentUser.isAdmin()
                && !userId.equals(info.getInterviewerId())
                && !userId.equals(info.getCandidateId())) {
            throw new ForbiddenException("Only the assigned interviewer or candidate can analyze code");
        }

        CodeAnalysis analysis = new CodeAnalysis();
        analysis.setSessionId(request.sessionId());
        analysis.setUserId(userId);
        analysis.setLanguage(request.language().toLowerCase());
        analysis.setCode(request.code());
        analysis.setStatus(AnalysisStatus.PENDING);
        analysis = repository.save(analysis);

        final UUID id = analysis.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    analysisExecutor.submit(() -> processInBackground(id));
                }
            });
        } else {
            analysisExecutor.submit(() -> processInBackground(id));
        }
        return analysis;
    }

    @Transactional(readOnly = true)
    public CodeAnalysis getById(UUID id) {
        CodeAnalysis a = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Analysis", id));
        ensureCanView(a);
        return a;
    }

    @Transactional(readOnly = true)
    public List<CodeAnalysis> listBySession(UUID sessionId, int limit) {
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(sessionId, CurrentUser.rawToken());
        if (info == null) throw new ForbiddenException("Session is not accessible to current user");
        int capped = Math.max(1, Math.min(limit, 200));
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, capped));
    }

    private void ensureCanView(CodeAnalysis a) {
        if (CurrentUser.isAdmin()) return;
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(a.getSessionId(), CurrentUser.rawToken());
        if (info == null) {
            throw new ForbiddenException("Analysis is not accessible to current user");
        }
    }

    // ----- background runner ------------------------------------------------

    private void processInBackground(UUID id) {
        CodeAnalysis loaded = transactionTemplate.execute(status -> {
            CodeAnalysis a = repository.findById(id).orElse(null);
            if (a == null) return null;
            a.setStatus(AnalysisStatus.RUNNING);
            return repository.save(a);
        });
        if (loaded == null) {
            log.warn("Analysis {} disappeared before run", id);
            return;
        }

        long startNs = System.nanoTime();
        AnalysisResult result;
        String errorMessage = null;
        try {
            Analyzer analyzer = analyzerRegistry.forLanguage(loaded.getLanguage());
            result = analyzer.analyze(loaded.getCode());
        } catch (Exception e) {
            log.error("Analyzer threw for analysis {}", id, e);
            result = null;
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;

        final AnalysisResult finalResult = result;
        final String finalError = errorMessage;
        CodeAnalysis finalRecord = transactionTemplate.execute(status -> {
            CodeAnalysis a = repository.findById(id).orElse(null);
            if (a == null) return null;
            if (finalResult != null) {
                a.setStatus(AnalysisStatus.COMPLETED);
                a.setLinesOfCode(finalResult.linesOfCode());
                a.setCyclomaticComplexity(finalResult.cyclomaticComplexity());
                a.setCognitiveComplexity(finalResult.cognitiveComplexity());
                a.setMethodCount(finalResult.methodCount());
                a.setIssues(objectMapper.valueToTree(finalResult.issues()));
            } else {
                a.setStatus(AnalysisStatus.ERROR);
                a.setErrorMessage(finalError);
            }
            a.setDurationMs(durationMs);
            return repository.save(a);
        });

        if (finalRecord != null) {
            int issueCount = (finalResult != null && finalResult.issues() != null) ? finalResult.issues().size() : 0;
            eventPublisher.publishAnalysisCompleted(new AnalysisCompletedEvent(
                    finalRecord.getId(),
                    finalRecord.getSessionId(),
                    finalRecord.getUserId(),
                    finalRecord.getLanguage(),
                    finalRecord.getStatus(),
                    finalRecord.getLinesOfCode(),
                    finalRecord.getCyclomaticComplexity(),
                    finalRecord.getCognitiveComplexity(),
                    finalRecord.getMethodCount(),
                    issueCount,
                    finalRecord.getDurationMs(),
                    finalRecord.getUpdatedAt()
            ));
        }
    }
}
