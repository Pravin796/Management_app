package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.UserPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPushTokenRepository extends JpaRepository<UserPushToken, Long> {

    Optional<UserPushToken> findByPushToken(String pushToken);

    List<UserPushToken> findByUserId(Long userId);

    List<UserPushToken> findByUserIdIn(Collection<Long> userIds);

    @Query("SELECT upt.pushToken FROM UserPushToken upt WHERE upt.user.id IN :userIds")
    List<String> findTokensByUserIds(@Param("userIds") Collection<Long> userIds);

    @Query("SELECT upt.pushToken FROM UserPushToken upt")
    List<String> findAllTokens();

    void deleteByPushToken(String pushToken);
}

