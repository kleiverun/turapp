package com.ole.turapp.controller;

import tools.jackson.databind.ObjectMapper;
import com.ole.turapp.dto.LoginRequest;
import com.ole.turapp.dto.LoginResponse;
import com.ole.turapp.dto.UserRegistrationRequest;
import com.ole.turapp.dto.UserResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.config.JwtAuthFilter;
import com.ole.turapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** JwtAuthFilter is excluded — it needs a real JwtService, which this slice doesn't load. */
@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void register_ReturnsCreatedWithToken() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("hiker@example.com", "password123", "Hiker");
        LoginResponse response = new LoginResponse(1L, "hiker@example.com", "Hiker", "USER", Instant.now(), "jwt-token");
        when(userService.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("hiker@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_DuplicateEmail_ReturnsBadRequest() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("hiker@example.com", "password123", "Hiker");
        when(userService.register(any()))
                .thenThrow(new IllegalArgumentException("A user with that email is already registered"));

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A user with that email is already registered"));
    }

    @Test
    void login_ReturnsOkWithToken() throws Exception {
        LoginRequest request = new LoginRequest("hiker@example.com", "password123");
        LoginResponse response = new LoginResponse(1L, "hiker@example.com", "Hiker", "USER", Instant.now(), "jwt-token");
        when(userService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_WrongPassword_ReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("hiker@example.com", "wrongpassword");
        when(userService.login(any())).thenThrow(new IllegalArgumentException("Wrong email or password"));

        mockMvc.perform(post("/api/users/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Wrong email or password"));
    }

    @Test
    void getUser_ReturnsUser() throws Exception {
        UserResponse response = new UserResponse(1L, "hiker@example.com", "Hiker", "USER", Instant.now());
        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Hiker"));
    }

    @Test
    void getUser_NotFound_ReturnsNotFound() throws Exception {
        when(userService.getUser(999L)).thenThrow(new NotFoundException("Did not find user"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Did not find user"));
    }
}
