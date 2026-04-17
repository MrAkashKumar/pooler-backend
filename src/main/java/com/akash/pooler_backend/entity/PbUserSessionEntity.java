package com.akash.pooler_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "pb_user")
public class PbUserSessionEntity extends BaseEntity{

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;


}
