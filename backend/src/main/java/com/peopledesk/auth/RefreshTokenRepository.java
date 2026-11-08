package com.peopledesk.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken t where t.tokenHash = :tokenHash")
    int deleteByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
