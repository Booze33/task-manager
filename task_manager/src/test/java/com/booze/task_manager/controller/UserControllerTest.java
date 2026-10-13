package com.booze.task_manager.controller;

import com.booze.task_manager.dto.UserStatsResponse;
import com.booze.task_manager.model.Role;
import com.booze.task_manager.model.User;
import com.booze.task_manager.model.UserPrincipal;
import com.booze.task_manager.service.StatsCacheService;
import com.booze.task_manager.service.UserStatsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {
    private final UserStatsService statsService = mock(UserStatsService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new UserController(statsService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        User user = new User();
        user.setId("user-1");
        user.setName("Alex");
        user.setEmail("alex@example.com");
        user.setRole(Role.USER);
        var principal = UserPrincipal.forToken(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void statsRouteReturnsContractAndCacheHeader() throws Exception {
        UserStatsResponse stats = new UserStatsResponse(
                2, 4, 50, 1, List.of(new UserStatsResponse.DailyCompletion("Mon", 2)));
        when(statsService.get("user-1")).thenReturn(new StatsCacheService.CachedStats(stats, false));

        mvc.perform(get("/api/users/me/stats"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Cache", "MISS"))
                .andExpect(jsonPath("$.completedToday").value(2))
                .andExpect(jsonPath("$.totalTasks").value(4))
                .andExpect(jsonPath("$.weekly[0].day").value("Mon"))
                .andExpect(jsonPath("$.weekly[0].value").value(2));
    }

    @Test
    void cacheInvalidationRouteReturnsNoContentAndUsesCurrentUser() throws Exception {
        mvc.perform(delete("/api/users/me/stats/cache"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(statsService).invalidate("user-1");
    }
}
