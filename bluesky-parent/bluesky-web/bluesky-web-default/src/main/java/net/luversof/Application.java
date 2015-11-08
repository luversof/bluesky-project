package net.luversof;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import net.luversof.core.context.BlueskyApplicationContextInitializer;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws Throwable {
		SpringApplication springApplication = new SpringApplication(Application.class);
		springApplication.addInitializers(new BlueskyApplicationContextInitializer());
		springApplication.run(args);
	}

}
