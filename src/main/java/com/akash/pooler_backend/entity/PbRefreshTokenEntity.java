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
@Table(name = "pb_refresh_token")
public class PbRefreshTokenEntity extends BaseEntity{

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "")
    private String created_at;


}
