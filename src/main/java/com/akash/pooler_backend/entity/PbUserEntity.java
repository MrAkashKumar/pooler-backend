package com.akash.pooler_backend.entity;

import com.akash.pooler_backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(
        name = "pb_user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
public class PbUserEntity extends BaseEntity{

    @Column(name = "name")
    private String name;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    @Column(name = "entity_id")
    private String entityId;

    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

}
