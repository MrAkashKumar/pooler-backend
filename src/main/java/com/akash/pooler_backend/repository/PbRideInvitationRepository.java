package com.akash.pooler_backend.repository;

import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Akash Kumar
 */
public interface PbRideInvitationRepository extends JpaRepository<PbRideInvitationEntity, Long> {

    Optional<PbRideInvitationEntity> findByEntityId(String entityId);

    List<PbRideInvitationEntity> findAllByReceiverEntityIdAndStatusOrderByCreatedAtDesc(
            String receiverEntityId, InvitationStatusEnums status);

    List<PbRideInvitationEntity> findAllBySenderEntityIdOrderByCreatedAtDesc(String senderEntityId);

    List<PbRideInvitationEntity> findAllByReceiverEntityIdOrderByCreatedAtDesc(String receiverEntityId);

    @Modifying
    @Query("""
            UPDATE PbRideInvitationEntity i
               SET i.status = 'DECLINED'
             WHERE i.status = :status
               AND i.expiresAt < :now
            """)
    int markExpired(@Param("status") InvitationStatusEnums status, @Param("now") Instant now);
}
