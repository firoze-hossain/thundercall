package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long>, JpaSpecificationExecutor<RefreshToken> {
    Optional<RefreshToken> findByUserId(Long userId);

    Optional<RefreshToken> findByToken(String token);
}
