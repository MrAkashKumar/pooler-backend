package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.MessageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * @author Akash Kumar
 */

@Entity
@Table(name = "pb_chat_messages", indexes = {
        @Index(name = "idx_chat_msg_thread", columnList = "thread_entity_id"),
        @Index(name = "idx_chat_msg_sender", columnList = "sender_user_id"),
        @Index(name = "idx_chat_msg_created", columnList = "created_at"),
        @Index(name = "idx_chat_msg_indexed", columnList = "is_indexed")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PbChatMessageEntity extends BaseEntity {

    @Column(name = "thread_entity_id", nullable = false)
    private String threadEntityId;

    @Column(name = "sender_user_id", nullable = false)
    private String senderUserId;

    @Column(name = "content", length = 1000, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType = MessageType.TEXT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "read_by_user_ids", columnDefinition = "jsonb")
    private List<String> readByUserIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reactions", columnDefinition = "jsonb")
    private Map<String, List<String>> reactions = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_indexed")
    private Boolean isIndexed = false;
}
