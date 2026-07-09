package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbChatMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PbChatMessageRepository extends JpaRepository<PbChatMessageEntity, String> {

    Page<PbChatMessageEntity> findByThreadEntityIdOrderByCreatedAtDesc(String threadEntityId, Pageable pageable);

    @Query("SELECT COUNT(m) FROM PbChatMessageEntity m WHERE m.threadEntityId = :threadId")
    long countByThreadId(@Param("threadId") String threadId);

    @Query("SELECT m FROM PbChatMessageEntity m WHERE m.threadEntityId = :threadId AND (m.content LIKE %:query% OR m.metadata ->> 'address' LIKE %:query%) ORDER BY m.createdAt DESC")
    List<PbChatMessageEntity> searchInThread(@Param("threadId") String threadId, @Param("query") String query);

    @Query("SELECT m FROM PbChatMessageEntity m WHERE m.threadEntityId = :threadId AND m.createdAt >= :startTime ORDER BY m.createdAt ASC")
    List<PbChatMessageEntity> findByThreadAndCreatedAfter(@Param("threadId") String threadId, @Param("startTime") Instant startTime);

    @Modifying
    @Query("DELETE FROM PbChatMessageEntity m WHERE m.threadEntityId = :threadId")
    void deleteByThreadId(@Param("threadId") String threadId);

    @Query(value = "SELECT COUNT(*) FROM pb_chat_messages WHERE thread_entity_id = :threadId AND (is_read = false OR :userId NOT IN (SELECT jsonb_array_elements_text(read_by_user_ids)))", nativeQuery = true)
    long countUnreadByThreadAndUser(@Param("threadId") String threadId, @Param("userId") String userId);
}