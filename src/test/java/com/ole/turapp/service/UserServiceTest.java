package com.ole.turapp.service;

import com.ole.turapp.config.JwtService;
import com.ole.turapp.dto.LoginRequest;
import com.ole.turapp.dto.LoginResponse;
import com.ole.turapp.dto.UserRegistrationRequest;
import com.ole.turapp.exception.NotFoundException;
import com.ole.turapp.model.Role;
import com.ole.turapp.model.User;
import com.ole.turapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private UserRegistrationRequest validRequest(String email, String password, String displayName) {
        return new UserRegistrationRequest(email, password, displayName);
    }
    private LoginRequest loginRequest(String email, String password) {
        return new LoginRequest(email, password);
    }
    @Test
    void testRegisterUser_Success() {
        // Arrange
        UserRegistrationRequest request = validRequest("email@gmail.com", "password123", "user1");

        User savedUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        LoginResponse response = userService.register(request);

        // Assert
        assertThat(response).isNotNull()
         .extracting(LoginResponse::displayName, LoginResponse::email)
        .containsExactly("user1", "email@gmail.com");
    }
    @Test
    void testLoginUser_Success() {
        // Arrange
        String email = "email@gmail.com";
        String rawPassword = "password123";
        String hashedPassword = "hashedPassword";
        User existingUser = new User(email, hashedPassword, "user1", Role.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

        LoginRequest request = loginRequest(email, rawPassword);

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertThat(response).isNotNull()
                .extracting(LoginResponse::displayName, LoginResponse::email)
                .containsExactly("user1", email);
    }
    @Test
    void testRegisterUser_BlankEmail_ThrowsException() {
        UserRegistrationRequest request = validRequest("", "gyldigPassord123", "Ole Kristian");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is required");
    }

    @Test
    void testRegisterUser_TooShortPassword_ThrowsException() {
        UserRegistrationRequest request = validRequest("ole@example.com", "pass12", "Ole Kristian");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid password");
    }

    @Test
    void testRegisterUser_BlankDisplayName_ThrowsException() {
        UserRegistrationRequest request = validRequest("ole@example.com", "gyldigPassord123", "");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is required");
    }

    @Test
    void testRegisterUser_EmailAlreadyRegistered_ThrowsException() {
        UserRegistrationRequest request = validRequest("ole@example.com", "gyldigPassord123", "Ole Kristian");
        when(userRepository.existsByEmail("ole@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A user with that email is already registered");
    }
    @Test
    void testLoginUser_EmptyEmail_ThrowsException(){
        LoginRequest request = loginRequest("","awd123");
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email or password is required");
    }
    @Test
    void testLoginUser_EmptyPassword_ThrowsException(){
        LoginRequest request = loginRequest("password@empty.com","");
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email or password is required");
    }
    @Test
    void testLoginUser_WrongEmail_ThrowsException(){
    /*    User user = new User("email@gmail.com", "passwortest123", "testUsername", "USER");
        user = userRepository.save(user);
    */
        LoginRequest request = loginRequest("password@empty.com","password123");
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wrong email or password");
    }
    @Test
    void testLoginUser_WrongPassword_ThrowsException() {
        // Arrange
        String email = "email@gmail.com";
        String storedPasswordHash = "encodedHashHer"; // representerer et allerede enkodet passord
        User existingUser = new User(email, storedPasswordHash, "testUsername", Role.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("feilPassord123", storedPasswordHash)).thenReturn(false);

        LoginRequest request = loginRequest(email, "feilPassord123");

        // Act & Assert
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wrong email or password");
    }
    @Test
    void testGetUser_DoesNotExist_ThrowsException() {
        // Arrange
        Long id = 900L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUser(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Did not find user");
    }
}