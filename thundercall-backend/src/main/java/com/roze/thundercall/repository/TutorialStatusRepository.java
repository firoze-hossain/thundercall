package com.roze.thundercall.repository;

import com.roze.thundercall.entity.TutorialStatus;
import com.roze.thundercall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TutorialStatusRepository extends JpaRepository<TutorialStatus, Long>, JpaSpecificationExecutor<TutorialStatus> {
    Optional<TutorialStatus> findByUser(User user);
}
