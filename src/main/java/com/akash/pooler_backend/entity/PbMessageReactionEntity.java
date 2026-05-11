package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "pb_message_reactions", indexes = {
        @Index(name = "idx_msg_reaction_msg_id", columnList = "message_entity_id"),
        @Index(name = "idx_msg_reaction_user_id", columnList = "user_entity_id"),
        @Index(name = "idx_msg_reaction_unique", columnList = "message_entity_id,user_entity_id,reaction", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbMessageReactionEntity extends BaseEntity {

    @Column(name = "message_entity_id", nullable = false)
    private String messageEntityId;

    @Column(name = "user_entity_id", nullable = false)
    private String userEntityId;

    @Column(name = "reaction", length = 4, nullable = false)
    private String reaction;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
