package com.datatrust.infrastructure.persistence.users;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    @Query("select u from UserEntity u where lower(u.username) = lower(:username)")
    Optional<UserEntity> findByUsernameIgnoreCase(@Param("username") String username);
}
