package com.diploma.user_service.repository;

import com.diploma.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("""
            SELECT u FROM User u
            WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:roleName IS NULL OR u.role.name = :roleName)
              AND (:companyId IS NULL OR u.company.id = :companyId)
            """)
    Page<User> search(
            @Param("q") String q,
            @Param("roleName") String roleName,
            @Param("companyId") UUID companyId,
            Pageable pageable
    );

    List<User> findAllByCompanyId(UUID companyId);
}
