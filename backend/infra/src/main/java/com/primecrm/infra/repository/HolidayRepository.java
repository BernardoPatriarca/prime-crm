package com.primecrm.infra.repository;

import com.primecrm.infra.entity.config.Holiday;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID>, JpaSpecificationExecutor<Holiday> {
}
