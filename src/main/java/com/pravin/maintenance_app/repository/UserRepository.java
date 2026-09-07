package com.pravin.maintenance_app.repository;

import com.pravin.maintenance_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMobileNumber(String mobileNumber);

    Optional<User> findByRoomId(Long roomId);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByRoomId(Long roomId);
}