package com.akash.pooler_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "pb_chat_archives", indexes = {
        @Index(name = "idx_archive_thread_id", columnList = "thread_entity_id", unique = true),
        @Index(name = "idx_archive_archived_at", columnList = "archived_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatArchiveEntity extends BaseEntity {

    @Column(name = "thread_entity_id", nullable = false, unique = true)
    private String threadEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "archive_data", columnDefinition = "jsonb", nullable = false)
    private Object archiveData;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @CreationTimestamp
    @Column(name = "archived_at", nullable = false, updatable = false)
    private Instant archivedAt;
}
