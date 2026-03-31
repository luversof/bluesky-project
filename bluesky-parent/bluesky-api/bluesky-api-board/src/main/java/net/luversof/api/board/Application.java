package net.luversof.api.board;

import io.github.luversof.boot.security.crypto.factory.TextEncryptorFactories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.encrypt.Encryptors;

@SpringBootApplication
public class Application {

    public static void main(String[] args) throws Throwable {
        TextEncryptorFactories.createDelegatingTextEncryptor()
                .addTextEncryptor("encType2", Encryptors.text("pass", "8560b4f4b3"));
        SpringApplication.run(Application.class, args);
    }
}
