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
@Table(name = "pb_user_entity")
public class PbUserEntity extends BaseEntity{

    @Column(name = "name")
    private String name;

    @Column(name = "entity_id")
    private String entityId;

}
