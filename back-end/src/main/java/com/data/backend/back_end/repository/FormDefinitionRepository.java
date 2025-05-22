package com.data.backend.back_end.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.data.backend.back_end.model.FormDefinition;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, String> {

    List<FormDefinition> findByAllowedRole(String allowedRole);
}
