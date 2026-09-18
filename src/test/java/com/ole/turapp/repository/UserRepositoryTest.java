package com.ole.turapp.repository;

import com.ole.turapp.model.Role;
import com.ole.turapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_ReturnsUser_WhenExists() {
        entityManager.persistAndFlush(new User("hiker@example.com", "hashedPw", "Hiker", Role.USER));

        Optional<User> found = userRepository.findByEmail("hiker@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Hiker");
    }

    @Test
    void findByEmail_ReturnsEmpty_WhenNotFound() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_TrueWhenRegistered_FalseOtherwise() {
        entityManager.persistAndFlush(new User("hiker@example.com", "hashedPw", "Hiker", Role.USER));

        assertThat(userRepository.existsByEmail("hiker@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("someone-else@example.com")).isFalse();
    }
}
