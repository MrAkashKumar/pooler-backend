package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "pb_chat_search_indexes", indexes = {
        @Index(name = "idx_search_idx_thread", columnList = "thread_entity_id", unique = true),
        @Index(name = "idx_search_idx_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatSearchIndexEntity extends BaseEntity {

    @Column(name = "thread_entity_id", nullable = false)
    private String threadEntityId;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "message_ids", columnDefinition = "jsonb")
    private List<String> messageIds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
