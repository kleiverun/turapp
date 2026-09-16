package com.ole.turapp.service;

import com.ole.turapp.config.JwtService;
import com.ole.turapp.dto.LoginRequest;
import com.ole.turapp.dto.LoginResponse;
import com.ole.turapp.dto.UserRegistrationRequest;
import com.ole.turapp.dto.UserResponse;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Role;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse register(UserRegistrationRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Invalid password");
        }
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("A user with that email is already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.displayName().trim(), Role.USER);
        user = userRepository.save(user);
        return toLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("Email or password is required");
        }

        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Wrong email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Wrong email or password");
        }
        return toLoginResponse(user);
    }

    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Did not find user" ));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name(), user.getCreatedAt());
    }

    private LoginResponse toLoginResponse(User user) {
        String token = jwtService.generate(user.getId());
        return new LoginResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name(), user.getCreatedAt(), token);
    }
}
