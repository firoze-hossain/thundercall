package com.roze.thundercall.api.repository;

import com.roze.thundercall.api.entity.RequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {
    List<RequestComment> findByRequestIdAndParentIsNullOrderByCreatedAtAsc(Long requestId);
}
