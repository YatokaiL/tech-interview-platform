package com.diploma.session_service.service;

import com.diploma.session_service.dto.TaskRequest;
import com.diploma.session_service.entity.Difficulty;
import com.diploma.session_service.entity.Task;
import com.diploma.session_service.entity.TaskCategory;
import com.diploma.session_service.exception.ForbiddenException;
import com.diploma.session_service.exception.NotFoundException;
import com.diploma.session_service.exception.ValidationException;
import com.diploma.session_service.repository.TaskCategoryRepository;
import com.diploma.session_service.repository.TaskRepository;
import com.diploma.session_service.repository.TaskSpecifications;
import com.diploma.session_service.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<Task> search(String q, Difficulty difficulty, String language, UUID categoryId, Pageable pageable) {
        UUID visibilityScope = CurrentUser.isAdmin() ? null : CurrentUser.companyId();
        Specification<Task> spec = Specification.allOf(
                TaskSpecifications.titleOrDescriptionContains(q),
                TaskSpecifications.hasDifficulty(difficulty),
                TaskSpecifications.hasLanguage(language),
                TaskSpecifications.hasCategory(categoryId),
                TaskSpecifications.visibleTo(visibilityScope)
        );
        return taskRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Task getById(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
        ensureCanView(task);
        return task;
    }

    @Transactional
    public Task create(TaskRequest request) {
        if (!CurrentUser.isAdmin() && !CurrentUser.isInterviewer() && !CurrentUser.isHrManager()) {
            throw new ForbiddenException("Only INTERVIEWER, HR_MANAGER or ADMIN can create tasks");
        }
        Task task = new Task();
        applyRequest(task, request);
        task.setCreatedBy(CurrentUser.id());
        task.setCompanyId(CurrentUser.isAdmin() ? null : CurrentUser.companyId());
        return taskRepository.save(task);
    }

    @Transactional
    public Task update(UUID id, TaskRequest request) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
        ensureCanEdit(task);
        applyRequest(task, request);
        return task;
    }

    @Transactional
    public void delete(UUID id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
        ensureCanEdit(task);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<TaskCategory> listCategories() {
        return categoryRepository.findAll();
    }

    private void applyRequest(Task task, TaskRequest request) {
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setDifficulty(request.difficulty());
        task.setLanguage(request.language());
        task.setInitialCode(request.initialCode());
        task.setExpectedOutput(request.expectedOutput());
        task.setTimeLimitMinutes(request.timeLimitMinutes());

        if (request.categoryIds() != null) {
            Set<TaskCategory> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
            if (categories.size() != request.categoryIds().size()) {
                throw new ValidationException("One or more category IDs are unknown");
            }
            task.setCategories(categories);
        }
    }

    private void ensureCanView(Task task) {
        if (CurrentUser.isAdmin()) return;
        if (task.getCompanyId() == null) return;
        UUID companyId = CurrentUser.companyId();
        if (companyId == null || !companyId.equals(task.getCompanyId())) {
            throw new ForbiddenException("Task does not belong to your company");
        }
    }

    private void ensureCanEdit(Task task) {
        if (CurrentUser.isAdmin()) return;
        UUID userId = CurrentUser.id();
        if (!task.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("Only the task author or an admin can modify it");
        }
    }
}
