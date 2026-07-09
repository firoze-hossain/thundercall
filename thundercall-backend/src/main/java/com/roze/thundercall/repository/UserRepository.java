package com.roze.thundercall.repository;

import com.roze.thundercall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByUsernameIgnoreCase(String username);

    Boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    @Query("select u from User u where u.username=:identifier or u.email=:identifier")
    Optional<User> findByUsernameOrEmail(@Param("identifier") String identifier);
}
