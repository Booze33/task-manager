package com.booze.task_manager.controller;

import com.booze.task_manager.model.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/greet")
    public String greet() {
        return "testing controllers";
    }
}
