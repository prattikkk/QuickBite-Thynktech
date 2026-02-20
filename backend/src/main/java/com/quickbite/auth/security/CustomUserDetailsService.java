package com.quickbite.auth.security;

import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Custom UserDetailsService implementation for loading user-specific data.
 * Used by Spring Security for authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email (username in our case).
     *
     * @param email user email
     * @return UserDetails
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!user.getActive()) {
            throw new UsernameNotFoundException("User account is deactivated: " + email);
        }

        return buildUserDetails(user);
    }

    /**
     * Convert User entity to Spring Security UserDetails.
     *
     * @param user User entity
     * @return UserDetails
     */
    private UserDetails buildUserDetails(User user) {
        List<GrantedAuthority> authorities = getAuthorities(user);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getId().toString())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getActive())
                .build();
    }

    /**
     * Convert user role to Spring Security authorities.
     * Role name is prefixed with "ROLE_" as per Spring Security convention.
     *
     * @param user User entity
     * @return list of granted authorities
     */
    private List<GrantedAuthority> getAuthorities(User user) {
        if (user.getRole() == null) {
            log.warn("User {} has no role assigned", user.getEmail());
            return Collections.emptyList();
        }

        String roleName = user.getRole().getName();
        
        // Ensure role has ROLE_ prefix
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        log.debug("User {} has authority: {}", user.getEmail(), roleName);
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }
}
