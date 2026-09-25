package com.firmaa.techdept.repositories;

import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("hashed");
        user.setRole(Role.ROLE_USER);
        return userRepository.save(user);
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        createUser("alice");

        Optional<User> result = userRepository.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    void findByUsername_nonExistingUser_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("ghost");

        assertFalse(result.isPresent());
    }

    @Test
    void existsByUsername_existingUser_returnsTrue() {
        createUser("bob");

        assertTrue(userRepository.existsByUsername("bob"));
    }

    @Test
    void existsByUsername_nonExistingUser_returnsFalse() {
        assertFalse(userRepository.existsByUsername("nobody"));
    }

    @Test
    void save_persistsUserCorrectly() {
        User saved = createUser("carol");

        assertNotNull(saved.getId());
        assertEquals("carol", saved.getUsername());
        assertEquals(Role.ROLE_USER, saved.getRole());
    }

    @Test
    void delete_removesUser() {
        User saved = createUser("dan");
        userRepository.delete(saved);

        assertFalse(userRepository.existsByUsername("dan"));
    }
}
