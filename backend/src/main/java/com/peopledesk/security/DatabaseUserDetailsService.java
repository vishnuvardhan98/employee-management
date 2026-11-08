package com.peopledesk.security;

import com.peopledesk.user.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads users and their BCrypt password hashes from the users table for login. */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsername(username)
                .map(u -> User.withUsername(u.getUsername())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
