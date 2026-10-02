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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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
        when(jwtService.generate(isNull())).thenReturn("test-token");

        // Act
        LoginResponse response = userService.register(request);

        // Assert
        verify(passwordEncoder).encode("password123");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User userToSave = userCaptor.getValue();
        assertThat(userToSave.getEmail()).isEqualTo("email@gmail.com");
        assertThat(userToSave.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(userToSave.getDisplayName()).isEqualTo("user1");
        assertThat(userToSave.getRole()).isEqualTo(Role.USER);
        verify(jwtService).generate(isNull());

        assertThat(response).isNotNull()
         .extracting(LoginResponse::displayName, LoginResponse::email, LoginResponse::token)
        .containsExactly("user1", "email@gmail.com", "test-token");
    }

    @Test
    void testRegisterUser_TrimsAndLowercasesEmail_TrimsDisplayName() {
        // Arrange
        UserRegistrationRequest request = validRequest(" Ole@EXAMPLE.com ", "password123", " Ole Kristian ");

        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.register(request);

        // Assert
        verify(userRepository).existsByEmail("ole@example.com");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("ole@example.com");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("Ole Kristian");
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
        when(jwtService.generate(isNull())).thenReturn("test-token");

        LoginRequest request = loginRequest(email, rawPassword);

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        verify(jwtService).generate(isNull());
        assertThat(response).isNotNull()
                .extracting(LoginResponse::displayName, LoginResponse::email, LoginResponse::token)
                .containsExactly("user1", email, "test-token");
    }
    @Test
    void testRegisterUser_BlankEmail_ThrowsException() {
        UserRegistrationRequest request = validRequest("", "validPassword123", "Ole Kristian");

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
        UserRegistrationRequest request = validRequest("ole@example.com", "validPassword123", "");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is required");
    }

    @Test
    void testRegisterUser_EmailAlreadyRegistered_ThrowsException() {
        UserRegistrationRequest request = validRequest("ole@example.com", "validPassword123", "Ole Kristian");
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
        String email = "password@empty.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginRequest request = loginRequest(email, "password123");
        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Wrong email or password");
    }
    @Test
    void testLoginUser_WrongPassword_ThrowsException() {
        // Arrange
        String email = "email@gmail.com";
        String storedPasswordHash = "encodedHashHere"; // represents an already-encoded password
        User existingUser = new User(email, storedPasswordHash, "testUsername", Role.USER);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword123", storedPasswordHash)).thenReturn(false);

        LoginRequest request = loginRequest(email, "wrongPassword123");

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

    @Test
    void testGetUser_Success() {
        // Arrange
        Long id = 1L;
        User existingUser = new User("email@gmail.com", "hashedPassword", "user1", Role.USER);
        ReflectionTestUtils.setField(existingUser, "id", id);

        when(userRepository.findById(id)).thenReturn(Optional.of(existingUser));

        // Act
        UserResponse response = userService.getUser(id);

        // Assert
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.email()).isEqualTo("email@gmail.com");
        assertThat(response.displayName()).isEqualTo("user1");
        assertThat(response.role()).isEqualTo("USER");
    }
}