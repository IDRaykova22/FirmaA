package com.firmaa.techdept;

import com.firmaa.techdept.models.Role;
import com.firmaa.techdept.models.User;
import com.firmaa.techdept.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds an initial admin account on first startup so there is always a way to
 * log in and create other users from the dashboard.  If an "admin" user already
 * exists the seeder does nothing.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Default admin account created (username: admin, password: admin)");
        }
        
        if (userRepository.findByUsername("worker").isEmpty()) {
            User worker = new User();
            worker.setUsername("worker");
            worker.setPassword(passwordEncoder.encode("worker"));
            worker.setRole(Role.ROLE_USER);
            worker.setJobTitle("Software Engineer");
            userRepository.save(worker);
            System.out.println(">>> Default worker account created (username: worker, password: worker)");
        }
    }
}
