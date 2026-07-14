package com.ole.turapp.service;

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

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse register(UserRegistrationRequest request) {
        if (request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("E-post er påkrevd");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new IllegalArgumentException("Passord må være minst 8 tegn");
        }
        if (request.displayName() == null || request.displayName().isBlank()) {
            throw new IllegalArgumentException("Visningsnavn er påkrevd");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-post er allerede registrert");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(email, passwordHash, request.displayName().trim(), Role.USER);

        return toResponse(userRepository.save(user));
    }

    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Fant ikke bruker med id " + userId));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}
