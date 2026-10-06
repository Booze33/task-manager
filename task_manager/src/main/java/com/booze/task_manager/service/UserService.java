package com.booze.task_manager.service;

import com.booze.task_manager.dto.LoginDTO;
import com.booze.task_manager.dto.RegisterDTO;
import com.booze.task_manager.dto.UserInfo;
import com.booze.task_manager.model.User;
import com.booze.task_manager.repo.UserRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;

    public UserService(UserRepo userRepo, PasswordEncoder encoder, AuthenticationManager authManager) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.authManager = authManager;
    }

    public UserInfo register(RegisterDTO input) {
        String email = normalize(input.getEmail());

        if(userRepo.findByEmail(email) != null) {
            throw new BadCredentialsException("Email is already in use");
        }

        User user = new User();
        user.setFull_name(input.getFull_name());
        user.setEmail(email);
        user.setPassword(encoder.encode(input.getPassword()));

        try {
            User saved = userRepo.save(user);

            return new UserInfo(saved.getId(), saved.getEmail());
        } catch (DataIntegrityViolationException e) {
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    private String normalize(String email) {
        return  email.trim().toLowerCase(Locale.ROOT);
    }

    public List<User> getUsers() {
        return userRepo.findAll();
    }

    public UserDetails login(LoginDTO input) {
        Authentication auth = authManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(normalize(input.getEmail()), input.getPassword()));

        return (UserDetails) auth.getPrincipal();
    }
}
