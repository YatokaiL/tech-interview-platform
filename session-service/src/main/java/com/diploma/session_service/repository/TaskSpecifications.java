package com.diploma.session_service.repository;

import com.diploma.session_service.entity.Difficulty;
import com.diploma.session_service.entity.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class TaskSpecifications {

    private TaskSpecifications() {}

    public static Specification<Task> titleOrDescriptionContains(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)
        );
    }

    public static Specification<Task> hasDifficulty(Difficulty difficulty) {
        if (difficulty == null) return null;
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Task> hasLanguage(String language) {
        if (language == null || language.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("language"), language);
    }

    public static Specification<Task> hasCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }

    /**
     * Visibility: a task is visible if it has no company (platform-wide)
     * or it belongs to the caller's company. ADMIN bypasses this with null.
     */
    public static Specification<Task> visibleTo(UUID companyId) {
        if (companyId == null) return null;
        return (root, query, cb) -> {
            Predicate noCompany = cb.isNull(root.get("companyId"));
            Predicate sameCompany = cb.equal(root.get("companyId"), companyId);
            return cb.or(noCompany, sameCompany);
        };
    }
}
