package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.Folder;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long>, JpaSpecificationExecutor<Folder> {
    List<Folder> findByCollectionWorkspaceOwner(User user);

    List<Folder> findByCollectionIdAndCollectionWorkspaceOwner(Long collectionId, User user);

    // Used for browsing a workspace shared with the caller — access is
    // checked separately before this is ever called.
    List<Folder> findByCollectionId(Long collectionId);

    // Bulk variant — fetches folders for EVERY collection in one query
    // instead of one query per collection. Used by
    // WorkspaceSharingService.getWorkspaceContents(), which used to
    // call findByCollectionId() in a loop — a classic N+1 that made
    // browsing a shared workspace with several collections noticeably
    // slow (one round-trip to the DB per collection, plus one more per
    // folder for its request count).
    List<Folder> findByCollectionIdIn(List<Long> collectionIds);

    // Bulk request-count — one GROUP BY query covering every folder at
    // once, instead of countRequestsByFolderId() called once per
    // folder in a loop. Returns [folderId, count] pairs.
    @Query("SELECT f.id, COUNT(r) FROM Folder f LEFT JOIN Request r ON r.folder = f WHERE f.id IN :folderIds GROUP BY f.id")
    List<Object[]> countRequestsByFolderIds(@Param("folderIds") List<Long> folderIds);

    Optional<Folder> findByIdAndCollectionWorkspaceOwner(Long id, User user);

    @Query("SELECT f FROM Folder f WHERE f.collection.workspace.owner = :user AND f.name = :name AND f.collection.id = :collectionId")
    Optional<Folder> findByNameAndCollectionIdAndCollectionWorkspaceOwner(
            @Param("name") String name,
            @Param("collectionId") Long collectionId,
            @Param("user") User user
    );

    // FIX: the old check above ignored parentFolderId, so two DIFFERENT
    // folders that legitimately share a name (e.g. "common" nested under
    // two different parents — completely normal in real API structures)
    // were wrongly rejected as duplicates. Uniqueness is scoped to the
    // parent (or "top-level" when parentFolderId is null) instead.
    @Query("SELECT f FROM Folder f WHERE f.collection.workspace.owner = :user AND f.name = :name "
            + "AND f.collection.id = :collectionId AND f.parentFolder.id = :parentFolderId")
    Optional<Folder> findByNameAndCollectionIdAndParentFolderIdAndCollectionWorkspaceOwner(
            @Param("name") String name,
            @Param("collectionId") Long collectionId,
            @Param("parentFolderId") Long parentFolderId,
            @Param("user") User user
    );

    @Query("SELECT f FROM Folder f WHERE f.collection.workspace.owner = :user AND f.name = :name "
            + "AND f.collection.id = :collectionId AND f.parentFolder IS NULL")
    Optional<Folder> findByNameAndCollectionIdAndParentFolderIsNullAndCollectionWorkspaceOwner(
            @Param("name") String name,
            @Param("collectionId") Long collectionId,
            @Param("user") User user
    );

    @Query("SELECT COUNT(r) FROM Request r WHERE r.folder.id = :folderId")
    Long countRequestsByFolderId(@Param("folderId") Long folderId);

    // Owner-agnostic versions of the duplicate-name checks above — used
    // when the caller's access to the collection has already been
    // verified via WorkspaceAccessGuard, so scoping strictly to the
    // collection ID is correct here (an Editor's duplicate folder name
    // should be rejected exactly like the owner's would be).
    @Query("SELECT f FROM Folder f WHERE f.name = :name AND f.collection.id = :collectionId AND f.parentFolder.id = :parentFolderId")
    Optional<Folder> findByNameAndCollectionIdAndParentFolderId(
            @Param("name") String name,
            @Param("collectionId") Long collectionId,
            @Param("parentFolderId") Long parentFolderId
    );

    @Query("SELECT f FROM Folder f WHERE f.name = :name AND f.collection.id = :collectionId AND f.parentFolder IS NULL")
    Optional<Folder> findByNameAndCollectionIdAndParentFolderIsNull(
            @Param("name") String name,
            @Param("collectionId") Long collectionId
    );

    @Query("SELECT f FROM Folder f WHERE f.name = :name AND f.collection.id = :collectionId")
    Optional<Folder> findByNameAndCollectionId(
            @Param("name") String name,
            @Param("collectionId") Long collectionId
    );
}