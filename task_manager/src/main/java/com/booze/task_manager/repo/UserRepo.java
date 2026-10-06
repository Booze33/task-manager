package com.booze.task_manager.repo;

import com.booze.task_manager.dto.UserInfo;
import com.booze.task_manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    User findByEmail(String email);
}
