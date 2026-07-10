package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.TutorialStatus;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TutorialStatusRepository extends JpaRepository<TutorialStatus, Long>, JpaSpecificationExecutor<TutorialStatus> {
    Optional<TutorialStatus> findByUser(User user);
}
