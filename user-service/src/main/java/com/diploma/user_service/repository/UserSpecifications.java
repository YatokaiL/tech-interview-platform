package com.diploma.user_service.repository;

import com.diploma.user_service.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> emailContains(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("email")), pattern);
    }

    public static Specification<User> hasRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role").get("name"), roleName);
    }

    public static Specification<User> hasCompany(UUID companyId) {
        if (companyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }
}
