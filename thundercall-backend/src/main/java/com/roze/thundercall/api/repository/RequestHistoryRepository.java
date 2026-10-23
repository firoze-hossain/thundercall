package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.RequestHistory;
import com.roze.thundercall.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long>, JpaSpecificationExecutor<RequestHistory> {
    @Query("SELECT h FROM RequestHistory h WHERE h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user) ORDER BY h.timestamp DESC")
    List<RequestHistory> findByUserOrWorkspaceOwnerOrderByTimestampDesc(@Param("user") User user);

    @Query("SELECT h FROM RequestHistory h WHERE h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user) ORDER BY h.timestamp DESC")
    Page<RequestHistory> findByUserOrWorkspaceOwnerOrderByTimestampDesc(@Param("user") User user, Pageable pageable);

    List<RequestHistory> findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(Long requestId, User user);

    Page<RequestHistory> findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(Long requestId, User user, Pageable pageable);

    @Query("SELECT h FROM RequestHistory h WHERE (h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user)) " +
            "AND h.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY h.timestamp DESC")
    List<RequestHistory> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(h) FROM RequestHistory h WHERE h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user)")
    Long countByUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RequestHistory h WHERE h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user)")
    void deleteAllForUser(@Param("user") User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RequestHistory h WHERE h.id = :id AND (h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user))")
    void deleteByIdAndUser(@Param("id") Long id, @Param("user") User user);

    void deleteByRequestCollectionWorkspaceOwner(User user);

    void deleteByRequestIdAndRequestCollectionWorkspaceOwner(Long requestId, User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM RequestHistory h WHERE (h.user = :user OR (h.request IS NOT NULL AND h.request.collection.workspace.owner = :user)) AND h.timestamp < :beforeDate")
    void deleteByUserAndTimestampBefore(@Param("user") User user, @Param("beforeDate") LocalDateTime beforeDate);
}
