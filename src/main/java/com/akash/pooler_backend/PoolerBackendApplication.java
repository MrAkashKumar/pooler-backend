package com.akash.pooler_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @author Akash Kumar
 */
@SpringBootApplication
@EnableJpaAuditing
@Configuration
public class PoolerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PoolerBackendApplication.class, args);
	}

}
