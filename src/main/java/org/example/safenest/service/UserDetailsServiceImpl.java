package org.example.safenest.service;

import org.example.safenest.model.User;
import org.example.safenest.repository.UserRepository;
import org.example.safenest.security.UserPrincipal;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;
    public UserDetailsServiceImpl(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return UserPrincipal.fromUser(u);
    }
}