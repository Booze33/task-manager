package com.booze.task_manager.controller;

import com.booze.task_manager.dto.LoginDTO;
import com.booze.task_manager.dto.RegisterDTO;
import com.booze.task_manager.dto.UserInfo;
import com.booze.task_manager.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserInfo> register(@RequestBody RegisterDTO input) {
        UserInfo user = userService.register(input);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDetails> login(@RequestBody LoginDTO input) {
        UserDetails user = userService.login(input);

        return new ResponseEntity<>(user, HttpStatus.OK);
    }
}
