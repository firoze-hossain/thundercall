package com.roze.thundercall.security;

import com.roze.thundercall.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String password;
    public Collection<? extends GrantedAuthority> authorities;
    private Boolean enabled;

    public static UserPrincipal create(User user) {
        GrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().name());
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                Collections.singleton(authority),
                user.getEnabled()
        );
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }
}
