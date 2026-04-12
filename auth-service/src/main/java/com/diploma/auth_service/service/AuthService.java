package com.diploma.auth_service.service;

import com.diploma.auth_service.dto.LoginRequest;
import com.diploma.auth_service.dto.RegisterRequest;
import com.diploma.auth_service.dto.TokenResponse;
import com.diploma.auth_service.dto.UserRegisteredEvent;
import com.diploma.auth_service.entity.Company;
import com.diploma.auth_service.entity.RefreshToken;
import com.diploma.auth_service.entity.Role;
import com.diploma.auth_service.entity.User;
import com.diploma.auth_service.exception.AccountBlockedException;
import com.diploma.auth_service.exception.InvalidCredentialsException;
import com.diploma.auth_service.exception.UserAlreadyExistsException;
import com.diploma.auth_service.exception.ValidationException;
import com.diploma.auth_service.kafka.UserEventPublisher;
import com.diploma.auth_service.repository.CompanyRepository;
import com.diploma.auth_service.repository.RoleRepository;
import com.diploma.auth_service.repository.UserRepository;
import com.diploma.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserEventPublisher userEventPublisher;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException(email);
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalStateException(
                        "Role not found in DB: " + request.role()));

        Company company = null;
        if (Role.HR_MANAGER.equals(request.role())) {
            if (!StringUtils.hasText(request.companyName())) {
                throw new ValidationException(
                        "companyName is required when registering as HR_MANAGER");
            }
            company = new Company();
            company.setName(request.companyName().trim());
            company = companyRepository.save(company);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(role);
        user.setCompany(company);
        user = userRepository.save(user);

        log.info("Registered user {} (role={})", user.getId(), role.getName());

        userEventPublisher.publishUserRegistered(new UserRegisteredEvent(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role.getName(),
                company != null ? company.getId() : null,
                user.getCreatedAt()
        ));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (user.isBlocked()) {
            throw new AccountBlockedException();
        }

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        JwtService.IssuedToken access = jwtService.issueAccessToken(rotated.getUser());
        return TokenResponse.bearer(access.value(), rotated.getToken(), access.expiresInSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new InvalidCredentialsException());
    }

    private TokenResponse issueTokens(User user) {
        JwtService.IssuedToken access = jwtService.issueAccessToken(user);
        RefreshToken refresh = refreshTokenService.issue(user);
        return TokenResponse.bearer(access.value(), refresh.getToken(), access.expiresInSeconds());
    }
}
