package com.diploma.execution_service.service;

import com.diploma.execution_service.client.SessionAccessClient;
import com.diploma.execution_service.dto.CodeExecutedEvent;
import com.diploma.execution_service.dto.ExecutionRequest;
import com.diploma.execution_service.entity.CodeExecution;
import com.diploma.execution_service.entity.ExecutionStatus;
import com.diploma.execution_service.exception.ForbiddenException;
import com.diploma.execution_service.exception.NotFoundException;
import com.diploma.execution_service.exception.ValidationException;
import com.diploma.execution_service.kafka.ExecutionEventPublisher;
import com.diploma.execution_service.repository.CodeExecutionRepository;
import com.diploma.execution_service.runner.DockerRunner;
import com.diploma.execution_service.runner.ExecutionResult;
import com.diploma.execution_service.runner.LanguageProfile;
import com.diploma.execution_service.runner.LanguageProfiles;
import com.diploma.execution_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final CodeExecutionRepository repository;
    private final LanguageProfiles profiles;
    private final DockerRunner runner;
    private final SessionAccessClient sessionAccessClient;
    private final ExecutorService executionExecutor;
    private final ExecutionEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public CodeExecution submit(ExecutionRequest request) {
        LanguageProfile profile = profiles.find(request.language())
                .orElseThrow(() -> new ValidationException("Unsupported language: " + request.language()));

        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(request.sessionId(), CurrentUser.rawToken());
        if (info == null) {
            throw new ForbiddenException("Session is not accessible to current user");
        }

        UUID userId = CurrentUser.id();
        if (!CurrentUser.isAdmin()
                && !userId.equals(info.getInterviewerId())
                && !userId.equals(info.getCandidateId())) {
            throw new ForbiddenException("Only the assigned interviewer or candidate can run code");
        }

        CodeExecution exec = new CodeExecution();
        exec.setSessionId(request.sessionId());
        exec.setUserId(userId);
        exec.setLanguage(profile.key());
        exec.setCode(request.code());
        exec.setStdin(request.stdin());
        exec.setStatus(ExecutionStatus.PENDING);
        exec = repository.save(exec);

        final UUID id = exec.getId();
        // Background run must start only after this @Transactional method commits — otherwise
        // the worker would race the commit and not see the new row.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executionExecutor.submit(() -> processInBackground(id, profile));
                }
            });
        } else {
            executionExecutor.submit(() -> processInBackground(id, profile));
        }
        return exec;
    }

    @Transactional(readOnly = true)
    public CodeExecution getById(UUID id) {
        CodeExecution exec = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Execution", id));
        ensureCanView(exec);
        return exec;
    }

    @Transactional(readOnly = true)
    public List<CodeExecution> listBySession(UUID sessionId, int limit) {
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(sessionId, CurrentUser.rawToken());
        if (info == null) throw new ForbiddenException("Session is not accessible to current user");
        int capped = Math.max(1, Math.min(limit, 200));
        return repository.findBySessionIdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, capped));
    }

    private void ensureCanView(CodeExecution exec) {
        if (CurrentUser.isAdmin()) return;
        SessionAccessClient.SessionInfo info = sessionAccessClient.fetchAccessible(exec.getSessionId(), CurrentUser.rawToken());
        if (info == null) {
            throw new ForbiddenException("Execution is not accessible to current user");
        }
    }

    // ----- background runner ------------------------------------------------

    private void processInBackground(UUID id, LanguageProfile profile) {
        CodeExecution loaded = transactionTemplate.execute(status -> {
            CodeExecution e = repository.findById(id).orElse(null);
            if (e == null) return null;
            e.setStatus(ExecutionStatus.RUNNING);
            return repository.save(e);
        });
        if (loaded == null) {
            log.warn("Execution {} disappeared before run", id);
            return;
        }

        ExecutionResult result;
        try {
            result = runner.run(profile, loaded.getCode(), loaded.getStdin());
        } catch (Exception e) {
            log.error("Runner threw for execution {}", id, e);
            result = new ExecutionResult(ExecutionStatus.ERROR, null, "", "", 0L,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        final ExecutionResult finalResult = result;
        CodeExecution finalRecord = transactionTemplate.execute(status -> {
            CodeExecution e = repository.findById(id).orElse(null);
            if (e == null) return null;
            e.setStatus(finalResult.status());
            e.setExitCode(finalResult.exitCode());
            e.setStdout(finalResult.stdout());
            e.setStderr(finalResult.stderr());
            e.setDurationMs(finalResult.durationMs());
            e.setErrorMessage(finalResult.errorMessage());
            return repository.save(e);
        });

        if (finalRecord != null) {
            eventPublisher.publishCodeExecuted(new CodeExecutedEvent(
                    finalRecord.getId(),
                    finalRecord.getSessionId(),
                    finalRecord.getUserId(),
                    finalRecord.getLanguage(),
                    finalRecord.getStatus(),
                    finalRecord.getExitCode(),
                    finalRecord.getDurationMs(),
                    finalRecord.getUpdatedAt()
            ));
        }
    }
}
