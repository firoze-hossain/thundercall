package com.roze.thundercall.repository;

import com.roze.thundercall.entity.RequestHistory;
import com.roze.thundercall.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long>, JpaSpecificationExecutor<RequestHistory> {
    List<RequestHistory> findByRequestCollectionWorkspaceOwnerOrderByTimestampDesc(User user);

    Page<RequestHistory> findByRequestCollectionWorkspaceOwnerOrderByTimestampDesc(User user, Pageable pageable);

    List<RequestHistory> findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(Long requestId, User user);

    Page<RequestHistory> findByRequestIdAndRequestCollectionWorkspaceOwnerOrderByTimestampDesc(Long requestId, User user, Pageable pageable);

    @Query("SELECT h FROM RequestHistory h WHERE h.request.collection.workspace.owner = :user " +
            "AND h.timestamp BETWEEN :startDate AND :endDate " +
            "ORDER BY h.timestamp DESC")
    List<RequestHistory> findByUserAndDateRange(
            @Param("user") User user,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(h) FROM RequestHistory h WHERE h.request.collection.workspace.owner = :user")
    Long countByUser(@Param("user") User user);

    void deleteByRequestCollectionWorkspaceOwner(User user);

    void deleteByRequestIdAndRequestCollectionWorkspaceOwner(Long requestId, User user);

    @Query("DELETE FROM RequestHistory h WHERE h.request.collection.workspace.owner = :user AND h.timestamp < :beforeDate")
    void deleteByUserAndTimestampBefore(@Param("user") User user, @Param("beforeDate") LocalDateTime beforeDate);

}
