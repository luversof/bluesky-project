package net.luversof;

import net.luversof.core.BlueskyApplicationContextInitializer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
//수정 중
    public static void main(String[] args) throws Throwable {
    	SpringApplication springApplication = new SpringApplication(Application.class);
    	springApplication.addInitializers(new BlueskyApplicationContextInitializer());
    	springApplication.run(args);
    }
// 마스터를 근데 수정함
}
