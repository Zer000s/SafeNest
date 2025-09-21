package org.example.safenest.service;

import org.example.safenest.exception.ApiException;
import org.example.safenest.model.User;
import org.example.safenest.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User getCurrentUser(Object principal) {
        if (principal == null) {
            throw new ApiException("User not authenticated", 401);
        }

        if (principal instanceof User) {
            return (User) principal;
        }
        else if (principal instanceof UserDetails) {
            String email = ((UserDetails) principal).getUsername();
            return userRepo.findByEmail(email)
                    .orElseThrow(() -> new ApiException("User not found", 404));
        }
        else {
            throw new ApiException("Cannot determine user", 500);
        }
    }
}