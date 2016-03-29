package net.luversof;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SpringBootApplication
@NoArgsConstructor(access=AccessLevel.PRIVATE)
public class TestApplication {
	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}
}
