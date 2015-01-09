package net.luversof;

import net.luversof.core.BlueskyApplicationContextInitializer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
// 이상하네
    public static void main(String[] args) throws Throwable {
    	SpringApplication springApplication = new SpringApplication(Application.class);
    	springApplication.addInitializers(new BlueskyApplicationContextInitializer());
    	springApplication.run(args);
    }

}
