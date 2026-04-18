package com.akash.pooler_backend.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Role enum with embedded Permission set.
 * Follows OCP — add new roles without touching existing code.
 */
@Getter
@RequiredArgsConstructor
public enum Role {

    ROLE_USER(Set.of(Permission.READ)),
    ROLE_MODERATOR(Set.of(Permission.READ, Permission.WRITE)),
    ROLE_ADMIN(Set.of(Permission.READ, Permission.WRITE, Permission.DELETE, Permission.MANAGE_USERS)),
    ROLE_SUPER_ADMIN(Set.of(Permission.READ, Permission.WRITE, Permission.DELETE,
            Permission.MANAGE_USERS, Permission.MANAGE_SYSTEM));

    private final Set<Permission> permissions;

    /** Spring Security GrantedAuthority list — includes both role and fine-grained permissions. */
    public List<SimpleGrantedAuthority> getAuthorities() {
        var list = permissions.stream()
                .map(p -> new SimpleGrantedAuthority(p.name()))
                .collect(Collectors.toList());
        list.add(new SimpleGrantedAuthority(this.name()));
        return list;
    }

    public enum Permission {
        READ,
        WRITE,
        DELETE,
        MANAGE_USERS,
        MANAGE_SYSTEM
    }
}
