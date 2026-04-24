package com.diploma.user_service.service;

import com.diploma.user_service.entity.Company;
import com.diploma.user_service.entity.User;
import com.diploma.user_service.exception.NotFoundException;
import com.diploma.user_service.repository.CompanyRepository;
import com.diploma.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Company getById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Company", id));
    }

    @Transactional(readOnly = true)
    public List<User> listUsers(UUID companyId) {
        if (!companyRepository.existsById(companyId)) {
            throw new NotFoundException("Company", companyId);
        }
        return userRepository.findAllByCompanyId(companyId);
    }
}
