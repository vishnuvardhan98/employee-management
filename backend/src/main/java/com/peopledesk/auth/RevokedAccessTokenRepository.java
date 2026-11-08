package com.peopledesk.auth;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, String> {

    @Modifying
    @Query("delete from RevokedAccessToken t where t.expiresAt < :now")
    int deleteExpired(LocalDateTime now);
}
