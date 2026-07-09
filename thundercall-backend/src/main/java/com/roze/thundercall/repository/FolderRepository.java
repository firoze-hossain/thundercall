package com.roze.thundercall.repository;

import com.roze.thundercall.entity.Folder;
import com.roze.thundercall.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long>, JpaSpecificationExecutor<Folder> {
    List<Folder> findByCollectionWorkspaceOwner(User user);

    List<Folder> findByCollectionIdAndCollectionWorkspaceOwner(Long collectionId, User user);

    Optional<Folder> findByIdAndCollectionWorkspaceOwner(Long id, User user);

    @Query("SELECT f FROM Folder f WHERE f.collection.workspace.owner = :user AND f.name = :name AND f.collection.id = :collectionId")
    Optional<Folder> findByNameAndCollectionIdAndCollectionWorkspaceOwner(
            @Param("name") String name,
            @Param("collectionId") Long collectionId,
            @Param("user") User user
    );

    @Query("SELECT COUNT(r) FROM Request r WHERE r.folder.id = :folderId")
    Long countRequestsByFolderId(@Param("folderId") Long folderId);
}