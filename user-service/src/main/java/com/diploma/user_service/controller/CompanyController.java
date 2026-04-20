package com.diploma.user_service.controller;

import com.diploma.user_service.dto.CompanyResponse;
import com.diploma.user_service.dto.UserResponse;
import com.diploma.user_service.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'INTERVIEWER')")
    public ResponseEntity<CompanyResponse> getOne(@PathVariable UUID id) {
        return ResponseEntity.ok(CompanyResponse.from(companyService.getById(id)));
    }

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<UserResponse>> listUsers(@PathVariable UUID id) {
        List<UserResponse> users = companyService.listUsers(id).stream()
                .map(UserResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
