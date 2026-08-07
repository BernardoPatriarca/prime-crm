package com.primecrm.infra.repository;

import com.primecrm.infra.entity.config.CustomField;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomFieldRepository
        extends JpaRepository<CustomField, UUID>, JpaSpecificationExecutor<CustomField> {
}
