package com.primecrm.infra.repository;

import com.primecrm.infra.entity.auth.RefreshToken;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, UUID>, JpaSpecificationExecutor<RefreshToken> {
}
