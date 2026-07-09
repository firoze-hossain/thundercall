package com.roze.thundercall.repository;

import com.roze.thundercall.entity.Request;
import com.roze.thundercall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long>, JpaSpecificationExecutor<Request> {
    @Query("select r from Request  r where  r.collection.workspace.owner=:user and r.name=:name")
    Optional<Request> findByNameAndCollectionWorkspaceOwner(@Param("name") String name, @Param("user") User owner);

    Optional<Request> findByIdAndCollectionWorkspaceOwner(Long id, User user);

    List<Request> findByCollectionId(Long id);
}
