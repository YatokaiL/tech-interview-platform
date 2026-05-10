package com.diploma.session_service.repository;

import com.diploma.session_service.entity.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TaskCategoryRepository extends JpaRepository<TaskCategory, UUID> {
}
