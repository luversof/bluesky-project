package net.luversof.web.gate;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.crypto.encrypt.Encryptors;

import io.github.luversof.boot.security.crypto.factory.BlueskyTextEncryptorFactories;

@SpringBootApplication
@EnableFeignClients
public class Application {

	public static void main(String[] args) throws Throwable {
		BlueskyTextEncryptorFactories.createDelegatingTextEncryptor(Map.of("encType2", Encryptors.text("pass", "8560b4f4b3")));
		SpringApplication.run(Application.class, args);
	}

}
