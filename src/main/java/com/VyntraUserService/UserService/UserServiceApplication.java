package com.VyntraUserService.UserService;

import com.VyntraUserService.UserService.model.User;
import com.VyntraUserService.UserService.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner dataLoader(UserRepository userRepository) {
		return args -> {
			if (userRepository.findByUsername("admin").isEmpty()) {
				User admin = User.builder()
						.username("admin")
						.password("password")
						.permissions(Set.of("VIEW_PRODUCTS", "MANAGE_ORDERS", "MANAGE_STOCK"))
						.build();
				userRepository.save(admin);
				System.out.println("Admin user created in UserService");
			}
			if (userRepository.findByUsername("user").isEmpty()) {
				User user = User.builder()
						.username("user")
						.password("password")
						.permissions(Set.of("VIEW_PRODUCTS"))
						.build();
				userRepository.save(user);
				System.out.println("Regular user created in UserService");
			}
		};
	}
}
