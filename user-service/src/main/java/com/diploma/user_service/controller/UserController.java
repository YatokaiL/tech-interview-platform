package com.diploma.user_service.controller;

import com.diploma.user_service.dto.PageResponse;
import com.diploma.user_service.dto.UpdateUserRequest;
import com.diploma.user_service.dto.UserLookupResponse;
import com.diploma.user_service.dto.UserResponse;
import com.diploma.user_service.entity.User;
import com.diploma.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<User> result = userService.search(q, role, companyId, pageable);
        return ResponseEntity.ok(PageResponse.from(result, UserResponse::from));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(UserResponse.from(userService.getById(userId)));
    }

    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'INTERVIEWER', 'HR_MANAGER')")
    public ResponseEntity<List<UserLookupResponse>> lookup(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "CANDIDATE") String role,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int size = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.ASC, "email"));
        List<UserLookupResponse> result = userService.search(q, role, null, pageable)
                .getContent().stream()
                .map(UserLookupResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.getById(id)));
    }

    @GetMapping("/{id}/profile")
    public ResponseEntity<UserLookupResponse> getPublicProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(UserLookupResponse.from(userService.getById(id)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(UserResponse.from(userService.update(id, request)));
    }

    private static Sort parseSort(String raw) {
        String[] parts = raw.split(",");
        String property = parts[0];
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }
}
