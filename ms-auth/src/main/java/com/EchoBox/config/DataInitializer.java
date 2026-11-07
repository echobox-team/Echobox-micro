package com.EchoBox.config;

import com.EchoBox.model.User;
import com.EchoBox.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                createUser(userRepository, passwordEncoder, "admin@echobox.com", "admin123", true, 1);
                createUser(userRepository, passwordEncoder, "alice@techinnovations.com", "password123", false, 2);
                createUser(userRepository, passwordEncoder, "bob@servicosglobais.com", "securepass", false, 3);
                createUser(userRepository, passwordEncoder, "diana@negocioslocais.com", "userpass", false, 4);
                createUser(userRepository, passwordEncoder, "charlie@solucoesdigitais.com", "mypassword", false, 5);
                createUser(userRepository, passwordEncoder, "eva@clienteprimeiro.com", "strongpass", false, 6);
                createUser(userRepository, passwordEncoder, "joao@echobox.com", "testpass", false, 1);
                createUser(userRepository, passwordEncoder, "sarah@techinnovations.com", "devpass", false, 2);
            }
        };
    }

    private void createUser(UserRepository repo, PasswordEncoder encoder, String email, String rawPass, boolean isAdmin, int companyId) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPass));
        u.setPicture("https://placehold.co/250?text=" + email);
        u.setIsAdmin(isAdmin);
        u.setCompanyId(companyId);
        repo.save(u);
    }
}
