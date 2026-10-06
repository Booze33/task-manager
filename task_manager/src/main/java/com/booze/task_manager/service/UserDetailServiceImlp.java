package com.booze.task_manager.service;

import com.booze.task_manager.model.User;
import com.booze.task_manager.repo.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImlp implements UserDetailsService {
    private final UserRepo userRepo;

    public UserDetailServiceImlp(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(username);

        if(user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return (UserDetails) user;
    }
}
