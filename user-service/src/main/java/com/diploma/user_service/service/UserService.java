package com.diploma.user_service.service;

import com.diploma.user_service.dto.UpdateUserRequest;
import com.diploma.user_service.entity.Role;
import com.diploma.user_service.entity.User;
import com.diploma.user_service.exception.NotFoundException;
import com.diploma.user_service.exception.ValidationException;
import com.diploma.user_service.repository.RoleRepository;
import com.diploma.user_service.repository.UserRepository;
import com.diploma.user_service.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public Page<User> search(String q, String role, UUID companyId, Pageable pageable) {
        Specification<User> spec = Specification
                .allOf(
                        UserSpecifications.emailContains(q),
                        UserSpecifications.hasRole(role),
                        UserSpecifications.hasCompany(companyId)
                );
        return userRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));
    }

    @Transactional
    public User update(UUID id, UpdateUserRequest request) {
        User user = getById(id);

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.role() != null) {
            Role role = roleRepository.findByName(request.role())
                    .orElseThrow(() -> new ValidationException("Unknown role: " + request.role()));
            user.setRole(role);
        }
        if (request.blocked() != null) {
            user.setBlocked(request.blocked());
        }

        log.info("Updated user {} (role={}, blocked={})",
                user.getId(), user.getRole().getName(), user.isBlocked());
        return user;
    }
}
