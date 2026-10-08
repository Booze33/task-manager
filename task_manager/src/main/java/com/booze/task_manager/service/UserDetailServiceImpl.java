package com.booze.task_manager.service;

import com.booze.task_manager.model.User;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {
    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
        return UserPrincipal.from(user);
    }

    public UserPrincipal loadById(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return UserPrincipal.forToken(user);
    }
}
