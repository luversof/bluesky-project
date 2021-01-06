package net.luversof;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
//@SpringCloudApplication
//@ComponentScan
public class Application {

	public static void main(String[] args) throws Throwable {
		SpringApplication.run(Application.class, args);
	}

}
