package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbMessageReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PbMessageReactionRepository extends JpaRepository<PbMessageReactionEntity, String> {

    List<PbMessageReactionEntity> findByMessageEntityId(String messageEntityId);

    @Modifying
    @Query("DELETE FROM PbMessageReactionEntity r WHERE r.messageEntityId = :messageId AND r.userEntityId = :userId AND r.reaction = :emoji")
    void deleteByMessageAndUserAndReaction(@Param("messageId") String messageId, @Param("userId") String userId, @Param("emoji") String emoji);

    @Query("SELECT COUNT(r) FROM PbMessageReactionEntity r WHERE r.messageEntityId = :messageId AND r.reaction = :emoji")
    long countByMessageAndReaction(@Param("messageId") String messageId, @Param("emoji") String emoji);
}
