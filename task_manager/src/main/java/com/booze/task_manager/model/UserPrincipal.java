package com.booze.task_manager.model;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {
    private final String id;
    private final String name;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Role role;
    private final Instant lockedUntil;

    private UserPrincipal(String id, String name, String email, String password, boolean enabled, Role role, Instant lockedUntil) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.role = role;
        this.lockedUntil = lockedUntil;
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                user.getRole(),
                user.getLockedUntil()
        );
    }

    public static UserPrincipal forToken(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                user.getRole(),
                user.getLockedUntil()
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
