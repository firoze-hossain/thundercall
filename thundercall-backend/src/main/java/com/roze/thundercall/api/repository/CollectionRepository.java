package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Collection;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long>, JpaSpecificationExecutor<Collection> {
    Optional<Collection> findByIdAndWorkspaceOwner(Long id, User user);

    List<Collection> findByWorkspaceOwner(User user);
}
