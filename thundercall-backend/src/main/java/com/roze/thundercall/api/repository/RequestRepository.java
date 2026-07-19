package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Request;
import com.roze.thundercall.api.entity.User;
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

    // Bulk variant — one query for every collection at once, same
    // reasoning as FolderRepository.findByCollectionIdIn().
    List<Request> findByCollectionIdIn(List<Long> collectionIds);

    // Used for executing a request from a shared workspace — access is
    // checked separately (WorkspaceSharingService.hasAccess) before this
    // is ever called, and this also guards against someone passing a
    // requestId that belongs to a completely different workspace.
    @Query("select r from Request r where r.id = :requestId and r.collection.workspace.id = :workspaceId")
    Optional<Request> findByIdAndWorkspaceId(@Param("requestId") Long requestId, @Param("workspaceId") Long workspaceId);
}
