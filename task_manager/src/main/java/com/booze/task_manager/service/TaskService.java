package com.booze.task_manager.service;

import com.booze.task_manager.dto.TaskRequest;
import com.booze.task_manager.model.Task;
import com.booze.task_manager.model.User;
import com.booze.task_manager.repo.TaskRepo;
import com.booze.task_manager.repo.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepo taskRepo;
    private final UserRepo userRepo;

    @Transactional
    public Task create(TaskRequest request) {
        User user = userRepo.findById(request.user_id())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Task task = new Task();
        task.setUser(user);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());

        return taskRepo.save(task);
    }
}
